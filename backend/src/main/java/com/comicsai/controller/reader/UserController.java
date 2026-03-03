package com.comicsai.controller.reader;

import com.comicsai.common.ApiResponse;
import com.comicsai.common.annotation.RequireAuth;
import com.comicsai.config.JwtInterceptor;
import com.comicsai.model.dto.LoginDTO;
import com.comicsai.model.dto.RechargeDTO;
import com.comicsai.model.dto.RegisterDTO;
import com.comicsai.model.vo.LoginVO;
import com.comicsai.model.vo.ProfileVO;
import com.comicsai.model.vo.UserVO;
import com.comicsai.service.BalanceService;
import com.comicsai.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reader")
public class UserController {

    private final UserService userService;
    private final BalanceService balanceService;

    public UserController(UserService userService, BalanceService balanceService) {
        this.userService = userService;
        this.balanceService = balanceService;
    }

    @PostMapping("/auth/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        UserVO user = userService.register(dto);
        return ApiResponse.success(user);
    }

    @PostMapping("/auth/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO loginVO = userService.login(dto);
        return ApiResponse.success(loginVO);
    }

    @PostMapping("/auth/logout")
    @RequireAuth
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userService.logout(token);
        }
        return ApiResponse.success();
    }

    @GetMapping("/user/profile")
    @RequireAuth
    public ApiResponse<ProfileVO> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        ProfileVO profile = balanceService.getProfile(userId);
        return ApiResponse.success(profile);
    }

    @PostMapping("/user/recharge")
    @RequireAuth
    public ApiResponse<BigDecimal> recharge(@Valid @RequestBody RechargeDTO dto,
                                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        BigDecimal newBalance = balanceService.recharge(userId, dto.getAmount());
        return ApiResponse.success(newBalance);
    }
}
