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
import com.comicsai.model.vo.RechargeRecordVO;
import com.comicsai.model.vo.UnlockRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BalanceService {

    private final UserMapper userMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final ContentUnlockMapper contentUnlockMapper;

    public BalanceService(UserMapper userMapper,
                          RechargeRecordMapper rechargeRecordMapper,
                          ContentUnlockMapper contentUnlockMapper) {
        this.userMapper = userMapper;
        this.rechargeRecordMapper = rechargeRecordMapper;
        this.contentUnlockMapper = contentUnlockMapper;
    }

    @Transactional
    public BigDecimal recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "充值金额必须大于0");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new EntityNotFoundException("用户", userId);
        }

        BigDecimal newBalance = user.getBalance().add(amount);
        user.setBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        RechargeRecord record = new RechargeRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setBalanceAfter(newBalance);
        record.setCreatedAt(LocalDateTime.now());
        rechargeRecordMapper.insert(record);

        return newBalance;
    }

    public ProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new EntityNotFoundException("用户", userId);
        }

        ProfileVO profile = new ProfileVO();
        profile.setId(user.getId());
        profile.setEmail(user.getEmail());
        profile.setNickname(user.getNickname());
        profile.setBalance(user.getBalance());

        // Recharge records (most recent first)
        LambdaQueryWrapper<RechargeRecord> rechargeQuery = new LambdaQueryWrapper<>();
        rechargeQuery.eq(RechargeRecord::getUserId, userId);
        rechargeQuery.orderByDesc(RechargeRecord::getCreatedAt);
        List<RechargeRecordVO> rechargeRecords = rechargeRecordMapper.selectList(rechargeQuery)
                .stream()
                .map(RechargeRecordVO::fromEntity)
                .toList();
        profile.setRechargeRecords(rechargeRecords);

        // Unlock/consumption records (most recent first)
        LambdaQueryWrapper<ContentUnlock> unlockQuery = new LambdaQueryWrapper<>();
        unlockQuery.eq(ContentUnlock::getUserId, userId);
        unlockQuery.orderByDesc(ContentUnlock::getUnlockedAt);
        List<UnlockRecordVO> unlockRecords = contentUnlockMapper.selectList(unlockQuery)
                .stream()
                .map(UnlockRecordVO::fromEntity)
                .toList();
        profile.setUnlockRecords(unlockRecords);

        return profile;
    }
}
