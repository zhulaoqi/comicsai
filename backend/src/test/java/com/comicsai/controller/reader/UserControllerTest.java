package com.comicsai.controller.reader;

import com.comicsai.common.JwtUtil;
import com.comicsai.common.exception.AuthenticationException;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.config.JwtInterceptor;
import com.comicsai.model.dto.LoginDTO;
import com.comicsai.model.dto.RechargeDTO;
import com.comicsai.model.dto.RegisterDTO;
import com.comicsai.model.vo.LoginVO;
import com.comicsai.model.vo.ProfileVO;
import com.comicsai.model.vo.UserVO;
import com.comicsai.service.BalanceService;
import com.comicsai.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private BalanceService balanceService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void register_shouldReturn200OnSuccess() throws Exception {
        RegisterDTO dto = new RegisterDTO("test@example.com", "password123", "TestUser");
        UserVO userVO = new UserVO(1L, "test@example.com", "TestUser", BigDecimal.ZERO);

        when(userService.register(any(RegisterDTO.class))).thenReturn(userVO);

        mockMvc.perform(post("/api/reader/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("TestUser"))
                .andExpect(jsonPath("$.data.balance").value(0));
    }

    @Test
    void register_shouldReturn400WhenEmailInvalid() throws Exception {
        RegisterDTO dto = new RegisterDTO("invalid-email", "password123", "TestUser");

        mockMvc.perform(post("/api/reader/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void register_shouldReturn400WhenPasswordTooShort() throws Exception {
        RegisterDTO dto = new RegisterDTO("test@example.com", "12345", "TestUser");

        mockMvc.perform(post("/api/reader/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void register_shouldReturn400WhenNicknameBlank() throws Exception {
        RegisterDTO dto = new RegisterDTO("test@example.com", "password123", "");

        mockMvc.perform(post("/api/reader/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void register_shouldReturn409WhenEmailExists() throws Exception {
        RegisterDTO dto = new RegisterDTO("existing@example.com", "password123", "TestUser");

        when(userService.register(any(RegisterDTO.class)))
                .thenThrow(new BusinessException(409, "该邮箱已注册"));

        mockMvc.perform(post("/api/reader/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("该邮箱已注册"));
    }

    @Test
    void login_shouldReturn200WithTokenOnSuccess() throws Exception {
        LoginDTO dto = new LoginDTO("test@example.com", "password123");
        UserVO userVO = new UserVO(1L, "test@example.com", "TestUser", BigDecimal.ZERO);
        LoginVO loginVO = new LoginVO("jwt-token-here", userVO);

        when(userService.login(any(LoginDTO.class))).thenReturn(loginVO);

        mockMvc.perform(post("/api/reader/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token-here"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    void login_shouldReturn401WhenCredentialsWrong() throws Exception {
        LoginDTO dto = new LoginDTO("test@example.com", "wrongpassword");

        when(userService.login(any(LoginDTO.class)))
                .thenThrow(new AuthenticationException("邮箱或密码错误"));

        mockMvc.perform(post("/api/reader/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void login_shouldReturn400WhenEmailBlank() throws Exception {
        LoginDTO dto = new LoginDTO("", "password123");

        mockMvc.perform(post("/api/reader/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void logout_shouldReturn200WhenAuthenticated() throws Exception {
        String token = "valid-jwt-token";

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(redisTemplate.hasKey("user:session:" + token)).thenReturn(true);

        mockMvc.perform(post("/api/reader/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService).logout(token);
    }

    @Test
    void logout_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/reader/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getProfile_shouldReturn200WhenAuthenticated() throws Exception {
        String token = "valid-jwt-token";
        ProfileVO profileVO = new ProfileVO();
        profileVO.setId(1L);
        profileVO.setEmail("test@example.com");
        profileVO.setNickname("TestUser");
        profileVO.setBalance(new BigDecimal("50.00"));
        profileVO.setRechargeRecords(Collections.emptyList());
        profileVO.setUnlockRecords(Collections.emptyList());

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(redisTemplate.hasKey("user:session:" + token)).thenReturn(true);
        when(balanceService.getProfile(1L)).thenReturn(profileVO);

        mockMvc.perform(get("/api/reader/user/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.balance").value(50.00))
                .andExpect(jsonPath("$.data.rechargeRecords").isArray())
                .andExpect(jsonPath("$.data.unlockRecords").isArray());
    }

    @Test
    void getProfile_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/reader/user/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void recharge_shouldReturn200OnSuccess() throws Exception {
        String token = "valid-jwt-token";
        RechargeDTO dto = new RechargeDTO(new BigDecimal("100.00"));

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(redisTemplate.hasKey("user:session:" + token)).thenReturn(true);
        when(balanceService.recharge(1L, new BigDecimal("100.00")))
                .thenReturn(new BigDecimal("150.00"));

        mockMvc.perform(post("/api/reader/user/recharge")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(150.00));
    }

    @Test
    void recharge_shouldReturn401WhenNotAuthenticated() throws Exception {
        RechargeDTO dto = new RechargeDTO(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/reader/user/recharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void recharge_shouldReturn400WhenAmountInvalid() throws Exception {
        String token = "valid-jwt-token";
        RechargeDTO dto = new RechargeDTO(new BigDecimal("0.00"));

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(redisTemplate.hasKey("user:session:" + token)).thenReturn(true);

        mockMvc.perform(post("/api/reader/user/recharge")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
