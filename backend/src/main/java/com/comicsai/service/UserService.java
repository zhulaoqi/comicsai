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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final String SESSION_KEY_PREFIX = "user:session:";
    private static final long SESSION_TTL_HOURS = 24;

    public UserService(UserMapper userMapper, JwtUtil jwtUtil,
                       RedisTemplate<String, Object> redisTemplate) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public UserVO register(RegisterDTO dto) {
        // Check email uniqueness
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getEmail, dto.getEmail());
        if (userMapper.selectCount(query) > 0) {
            throw new BusinessException(409, "该邮箱已注册");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setBalance(BigDecimal.ZERO);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        return UserVO.fromUser(user);
    }

    public LoginVO login(LoginDTO dto) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getEmail, dto.getEmail());
        User user = userMapper.selectOne(query);

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("邮箱或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        // Store session in Redis
        redisTemplate.opsForValue().set(
                SESSION_KEY_PREFIX + token,
                user.getId(),
                SESSION_TTL_HOURS,
                TimeUnit.HOURS
        );

        return new LoginVO(token, UserVO.fromUser(user));
    }

    public void logout(String token) {
        if (token != null) {
            redisTemplate.delete(SESSION_KEY_PREFIX + token);
        }
    }

    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new EntityNotFoundException("用户不存在");
        }
        return UserVO.fromUser(user);
    }
}
