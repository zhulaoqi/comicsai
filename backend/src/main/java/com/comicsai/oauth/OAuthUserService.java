package com.comicsai.oauth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comicsai.mapper.OAuthAccountMapper;
import com.comicsai.mapper.UserMapper;
import com.comicsai.model.entity.OAuthAccount;
import com.comicsai.model.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class OAuthUserService {

    private final UserMapper userMapper;
    private final OAuthAccountMapper oauthAccountMapper;

    public OAuthUserService(UserMapper userMapper, OAuthAccountMapper oauthAccountMapper) {
        this.userMapper = userMapper;
        this.oauthAccountMapper = oauthAccountMapper;
    }

    @Transactional
    public User findOrCreateUser(String provider, String providerUserId, String email, String nickname) {
        // Check existing oauth binding
        LambdaQueryWrapper<OAuthAccount> oauthQuery = new LambdaQueryWrapper<>();
        oauthQuery.eq(OAuthAccount::getProvider, provider)
                  .eq(OAuthAccount::getProviderUserId, providerUserId);
        OAuthAccount oauthAccount = oauthAccountMapper.selectOne(oauthQuery);

        if (oauthAccount != null) {
            return userMapper.selectById(oauthAccount.getUserId());
        }

        // Try to find existing user by email
        User user = null;
        if (email != null && !email.isBlank()) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.eq(User::getEmail, email);
            user = userMapper.selectOne(userQuery);
        }

        // Create new user if not found
        if (user == null) {
            user = new User();
            user.setEmail(email != null && !email.isBlank()
                    ? email
                    : provider + "_" + providerUserId + "@oauth.local");
            user.setPasswordHash("");
            user.setNickname(nickname != null && !nickname.isBlank() ? nickname : "用户");
            user.setBalance(BigDecimal.ZERO);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }

        // Bind oauth account
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
