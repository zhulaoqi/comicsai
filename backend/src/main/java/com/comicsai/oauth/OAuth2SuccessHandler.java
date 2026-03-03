package com.comicsai.oauth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comicsai.common.JwtUtil;
import com.comicsai.mapper.OAuthAccountMapper;
import com.comicsai.mapper.UserMapper;
import com.comicsai.model.entity.OAuthAccount;
import com.comicsai.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserMapper userMapper;
    private final OAuthAccountMapper oauthAccountMapper;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_KEY_PREFIX = "user:session:";
    private static final long SESSION_TTL_HOURS = 24;

    @Value("${app.oauth.frontend-callback-url:http://localhost:5173}")
    private String frontendUrl;

    public OAuth2SuccessHandler(UserMapper userMapper, OAuthAccountMapper oauthAccountMapper,
                                JwtUtil jwtUtil, RedisTemplate<String, Object> redisTemplate) {
        this.userMapper = userMapper;
        this.oauthAccountMapper = oauthAccountMapper;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = token.getPrincipal();
        String provider = token.getAuthorizedClientRegistrationId(); // "github" or "google"

        String providerUserId;
        String email;
        String nickname;

        if ("github".equals(provider)) {
            Integer githubId = oauthUser.getAttribute("id");
            providerUserId = githubId != null ? String.valueOf(githubId) : oauthUser.getName();
            email = oauthUser.getAttribute("email");
            String name = oauthUser.getAttribute("name");
            String login = oauthUser.getAttribute("login");
            nickname = (name != null && !name.isBlank()) ? name : login;
        } else { // google
            providerUserId = oauthUser.getAttribute("sub");
            email = oauthUser.getAttribute("email");
            nickname = oauthUser.getAttribute("name");
        }

        User user = findOrCreateUser(provider, providerUserId, email, nickname);

        String jwt = jwtUtil.generateToken(user.getId(), user.getEmail());
        redisTemplate.opsForValue().set(
                SESSION_KEY_PREFIX + jwt, user.getId(),
                SESSION_TTL_HOURS, TimeUnit.HOURS);

        // Redirect to frontend with token
        String redirectUrl = frontendUrl + "/oauth/callback?token="
                + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
                + "&nickname=" + URLEncoder.encode(user.getNickname(), StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }

    private User findOrCreateUser(String provider, String providerUserId, String email, String nickname) {
        LambdaQueryWrapper<OAuthAccount> oauthQuery = new LambdaQueryWrapper<>();
        oauthQuery.eq(OAuthAccount::getProvider, provider)
                  .eq(OAuthAccount::getProviderUserId, providerUserId);
        OAuthAccount oauthAccount = oauthAccountMapper.selectOne(oauthQuery);

        if (oauthAccount != null) {
            return userMapper.selectById(oauthAccount.getUserId());
        }

        // Try to find by email
        User user = null;
        if (email != null) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.eq(User::getEmail, email);
            user = userMapper.selectOne(userQuery);
        }

        if (user == null) {
            user = new User();
            user.setEmail(email != null ? email : provider + "_" + providerUserId + "@oauth.local");
            user.setPasswordHash("");
            user.setNickname(nickname != null ? nickname : "用户");
            user.setBalance(BigDecimal.ZERO);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }

        OAuthAccount newOauth = new OAuthAccount();
        newOauth.setUserId(user.getId());
        newOauth.setProvider(provider);
        newOauth.setProviderUserId(providerUserId);
        newOauth.setEmail(email);
        newOauth.setNickname(nickname);
        newOauth.setCreatedAt(LocalDateTime.now());
        oauthAccountMapper.insert(newOauth);

        return user;
    }
}
