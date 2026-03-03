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

        // Check if the method or class requires authentication
        boolean requireAuth = handlerMethod.hasMethodAnnotation(RequireAuth.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAuth.class);

        String token = extractToken(request);

        // If token is present, always try to parse and set user context
        if (token != null && jwtUtil.validateToken(token)) {
            // Check if session exists in Redis
            Boolean sessionExists = redisTemplate.hasKey(SESSION_KEY_PREFIX + token);
            if (Boolean.TRUE.equals(sessionExists)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                request.setAttribute(USER_ID_ATTR, userId);

                if (!requireAuth) {
                    return true;
                }
                return true;
            }
        }

        // If auth is required but no valid token/session, reject
        if (requireAuth) {
            throw new AuthenticationException("未认证，请先登录");
        }

        // No auth required, allow through (guest access)
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
