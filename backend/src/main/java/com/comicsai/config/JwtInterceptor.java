package com.comicsai.config;

import com.comicsai.common.JwtUtil;
import com.comicsai.common.annotation.RequireAuth;
import com.comicsai.common.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    public static final String USER_ID_ATTR = "currentUserId";
    private static final String SESSION_KEY_PREFIX = "user:session:";
    private static final String ADMIN_SESSION_PREFIX = "admin:session:";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtInterceptor(JwtUtil jwtUtil, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean requireAuth = handlerMethod.hasMethodAnnotation(RequireAuth.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAuth.class);

        boolean isAdminPath = request.getRequestURI().startsWith("/api/admin/");
        String token = extractToken(request);

        if (token != null && jwtUtil.validateToken(token)) {
            if (isAdminPath) {
                // Admin path: validate against admin session store
                Boolean adminSession = redisTemplate.hasKey(ADMIN_SESSION_PREFIX + token);
                if (Boolean.TRUE.equals(adminSession)) {
                    return true;
                }
                throw new AuthenticationException("管理员未登录或登录已过期");
            } else {
                // Reader path: validate against user session store
                Boolean sessionExists = redisTemplate.hasKey(SESSION_KEY_PREFIX + token);
                if (Boolean.TRUE.equals(sessionExists)) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    request.setAttribute(USER_ID_ATTR, userId);
                    return true;
                }
            }
        }

        if (requireAuth || isAdminPath) {
            throw new AuthenticationException("未认证，请先登录");
        }

        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
