package com.comicsai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.common.exception.EntityNotFoundException;
import com.comicsai.mapper.ContentUnlockMapper;
import com.comicsai.mapper.RechargeRecordMapper;
import com.comicsai.mapper.UserMapper;
import com.comicsai.model.entity.ContentUnlock;
import com.comicsai.model.entity.RechargeRecord;
import com.comicsai.model.entity.User;
import com.comicsai.model.vo.ProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RechargeRecordMapper rechargeRecordMapper;

    @Mock
    private ContentUnlockMapper contentUnlockMapper;

    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceService(userMapper, rechargeRecordMapper, contentUnlockMapper);
    }

    // ==================== Recharge Tests ====================

    @Test
    void recharge_shouldIncreaseBalanceAndCreateRecord() {
        User user = buildUser(1L, new BigDecimal("100.00"));
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(rechargeRecordMapper.insert(any(RechargeRecord.class))).thenReturn(1);

        BigDecimal newBalance = balanceService.recharge(1L, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("150.00"), newBalance);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(new BigDecimal("150.00"), userCaptor.getValue().getBalance());

        ArgumentCaptor<RechargeRecord> recordCaptor = ArgumentCaptor.forClass(RechargeRecord.class);
        verify(rechargeRecordMapper).insert(recordCaptor.capture());
        RechargeRecord record = recordCaptor.getValue();
        assertEquals(1L, record.getUserId());
        assertEquals(new BigDecimal("50.00"), record.getAmount());
        assertEquals(new BigDecimal("150.00"), record.getBalanceAfter());
        assertNotNull(record.getCreatedAt());
    }

    @Test
    void recharge_shouldWorkWithZeroInitialBalance() {
        User user = buildUser(1L, BigDecimal.ZERO);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(rechargeRecordMapper.insert(any(RechargeRecord.class))).thenReturn(1);

        BigDecimal newBalance = balanceService.recharge(1L, new BigDecimal("25.50"));

        assertEquals(new BigDecimal("25.50"), newBalance);
    }

    @Test
    void recharge_shouldThrowWhenAmountIsNull() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> balanceService.recharge(1L, null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void recharge_shouldThrowWhenAmountIsZero() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> balanceService.recharge(1L, BigDecimal.ZERO));
        assertEquals(400, ex.getCode());
    }

    @Test
    void recharge_shouldThrowWhenAmountIsNegative() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> balanceService.recharge(1L, new BigDecimal("-10.00")));
        assertEquals(400, ex.getCode());
    }

    @Test
    void recharge_shouldThrowWhenUserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class,
                () -> balanceService.recharge(999L, new BigDecimal("50.00")));
    }

    // ==================== Profile Tests ====================

    @Test
    void getProfile_shouldReturnProfileWithRecords() {
        User user = buildUser(1L, new BigDecimal("200.00"));
        user.setEmail("test@example.com");
        user.setNickname("TestUser");
        when(userMapper.selectById(1L)).thenReturn(user);

        RechargeRecord rechargeRecord = new RechargeRecord();
        rechargeRecord.setId(10L);
        rechargeRecord.setUserId(1L);
        rechargeRecord.setAmount(new BigDecimal("200.00"));
        rechargeRecord.setBalanceAfter(new BigDecimal("200.00"));
        rechargeRecord.setCreatedAt(LocalDateTime.now());
        when(rechargeRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(rechargeRecord));

        ContentUnlock unlock = new ContentUnlock();
        unlock.setId(20L);
        unlock.setUserId(1L);
        unlock.setContentId(5L);
        unlock.setPricePaid(new BigDecimal("9.99"));
        unlock.setUnlockedAt(LocalDateTime.now());
        when(contentUnlockMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(unlock));

        ProfileVO profile = balanceService.getProfile(1L);

        assertEquals(1L, profile.getId());
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("TestUser", profile.getNickname());
        assertEquals(new BigDecimal("200.00"), profile.getBalance());
        assertEquals(1, profile.getRechargeRecords().size());
        assertEquals(new BigDecimal("200.00"), profile.getRechargeRecords().get(0).getAmount());
        assertEquals(1, profile.getUnlockRecords().size());
        assertEquals(5L, profile.getUnlockRecords().get(0).getContentId());
        assertEquals(new BigDecimal("9.99"), profile.getUnlockRecords().get(0).getPricePaid());
    }

    @Test
    void getProfile_shouldReturnEmptyRecordsForNewUser() {
        User user = buildUser(1L, BigDecimal.ZERO);
        user.setEmail("new@example.com");
        user.setNickname("NewUser");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(rechargeRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(contentUnlockMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        ProfileVO profile = balanceService.getProfile(1L);

        assertEquals(BigDecimal.ZERO, profile.getBalance());
        assertTrue(profile.getRechargeRecords().isEmpty());
        assertTrue(profile.getUnlockRecords().isEmpty());
    }

    @Test
    void getProfile_shouldThrowWhenUserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class,
                () -> balanceService.getProfile(999L));
    }

    // ==================== Helper ====================

    private User buildUser(Long id, BigDecimal balance) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setNickname("User" + id);
        user.setBalance(balance);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
