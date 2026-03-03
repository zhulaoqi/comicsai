package com.comicsai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comicsai.common.JwtUtil;
import com.comicsai.common.exception.AuthenticationException;
import com.comicsai.mapper.AdminUserMapper;
import com.comicsai.model.entity.AdminUser;
import com.comicsai.model.vo.AdminLoginVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String ADMIN_SESSION_PREFIX = "admin:session:";
    private static final long SESSION_TTL_HOURS = 24;

    public AdminAuthService(AdminUserMapper adminUserMapper, JwtUtil jwtUtil,
                            RedisTemplate<String, Object> redisTemplate) {
        this.adminUserMapper = adminUserMapper;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    public AdminLoginVO login(String email, String password) {
        LambdaQueryWrapper<AdminUser> query = new LambdaQueryWrapper<>();
        query.eq(AdminUser::getEmail, email);
        AdminUser admin = adminUserMapper.selectOne(query);

        if (admin == null || !passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new AuthenticationException("邮箱或密码错误");
        }

        // Use negative id to distinguish admin tokens from user tokens
        String token = jwtUtil.generateToken(-admin.getId(), admin.getEmail());

        redisTemplate.opsForValue().set(
                ADMIN_SESSION_PREFIX + token,
                admin.getId(),
                SESSION_TTL_HOURS,
                TimeUnit.HOURS
        );

        return new AdminLoginVO(token, admin.getId(), admin.getEmail(), admin.getNickname());
    }

    public void logout(String token) {
        redisTemplate.delete(ADMIN_SESSION_PREFIX + token);
    }

    /**
     * Validate admin token and return admin id, or null if invalid.
     */
    public Long validateAdminToken(String token) {
        if (token == null || !jwtUtil.validateToken(token)) return null;
        Object val = redisTemplate.opsForValue().get(ADMIN_SESSION_PREFIX + token);
        if (val == null) return null;
        return val instanceof Integer ? ((Integer) val).longValue() : (Long) val;
    }
}
