package com.comicsai.controller.admin;

import com.comicsai.common.ApiResponse;
import com.comicsai.model.dto.LoginDTO;
import com.comicsai.model.vo.AdminLoginVO;
import com.comicsai.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginVO> login(@Valid @RequestBody LoginDTO dto) {
        AdminLoginVO vo = adminAuthService.login(dto.getEmail(), dto.getPassword());
        return ApiResponse.success(vo);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            adminAuthService.logout(auth.substring(7));
        }
        return ApiResponse.success();
    }
}
