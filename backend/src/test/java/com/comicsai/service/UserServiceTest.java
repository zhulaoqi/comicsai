package com.comicsai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comicsai.common.JwtUtil;
import com.comicsai.common.exception.AuthenticationException;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.common.exception.EntityNotFoundException;
import com.comicsai.mapper.UserMapper;
import com.comicsai.model.dto.LoginDTO;
import com.comicsai.model.dto.RegisterDTO;
import com.comicsai.model.entity.User;
import com.comicsai.model.vo.LoginVO;
import com.comicsai.model.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, jwtUtil, redisTemplate);
    }

    @Test
    void register_shouldCreateUserWithEncryptedPassword() {
        RegisterDTO dto = new RegisterDTO("test@example.com", "password123", "TestUser");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        UserVO result = userService.register(dto);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("TestUser", result.getNickname());
        assertEquals(BigDecimal.ZERO, result.getBalance());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertNotEquals("password123", savedUser.getPasswordHash());
        assertNotNull(savedUser.getPasswordHash());
    }

    @Test
    void register_shouldThrow409WhenEmailExists() {
        RegisterDTO dto = new RegisterDTO("existing@example.com", "password123", "TestUser");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(dto));
        assertEquals(409, ex.getCode());
        assertEquals("该邮箱已注册", ex.getMessage());
    }

    @Test
    void register_shouldSetInitialBalanceToZero() {
        RegisterDTO dto = new RegisterDTO("new@example.com", "password123", "NewUser");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        UserVO result = userService.register(dto);
        assertEquals(BigDecimal.ZERO, result.getBalance());
    }

    @Test
    void login_shouldReturnTokenAndUserInfo() {
        LoginDTO dto = new LoginDTO("test@example.com", "password123");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setNickname("TestUser");
        user.setBalance(BigDecimal.ZERO);
        // BCrypt hash of "password123"
        user.setPasswordHash("$2a$10$dummyhash");

        // We need a real BCrypt hash for the test to work
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        user.setPasswordHash(encoder.encode("password123"));

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(jwtUtil.generateToken(1L, "test@example.com")).thenReturn("test-jwt-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        LoginVO result = userService.login(dto);

        assertNotNull(result);
        assertEquals("test-jwt-token", result.getToken());
        assertEquals("test@example.com", result.getUser().getEmail());
        assertEquals("TestUser", result.getUser().getNickname());

        verify(valueOperations).set(eq("user:session:test-jwt-token"), eq(1L), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void login_shouldThrowWhenEmailNotFound() {
        LoginDTO dto = new LoginDTO("nonexistent@example.com", "password123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        AuthenticationException ex = assertThrows(AuthenticationException.class, () -> userService.login(dto));
        assertEquals("邮箱或密码错误", ex.getMessage());
    }

    @Test
    void login_shouldThrowWhenPasswordWrong() {
        LoginDTO dto = new LoginDTO("test@example.com", "wrongpassword");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        user.setPasswordHash(encoder.encode("correctpassword"));

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        AuthenticationException ex = assertThrows(AuthenticationException.class, () -> userService.login(dto));
        assertEquals("邮箱或密码错误", ex.getMessage());
    }

    @Test
    void logout_shouldDeleteRedisSession() {
        userService.logout("some-token");
        verify(redisTemplate).delete("user:session:some-token");
    }

    @Test
    void logout_shouldHandleNullToken() {
        userService.logout(null);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void getProfile_shouldReturnUserVO() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setNickname("TestUser");
        user.setBalance(new BigDecimal("100.00"));

        when(userMapper.selectById(1L)).thenReturn(user);

        UserVO result = userService.getProfile(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("TestUser", result.getNickname());
        assertEquals(new BigDecimal("100.00"), result.getBalance());
    }

    @Test
    void getProfile_shouldThrowWhenUserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> userService.getProfile(999L));
    }
}
