package com.bankos.modules.auth.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.modules.auth.dto.LoginRequest;
import com.bankos.modules.auth.dto.LoginResponse;
import com.bankos.modules.auth.dto.MeResponse;
import com.bankos.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(
                "LOGIN_SUCCESS",
                "Sesión iniciada correctamente",
                authService.login(request)
        );
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
        return ApiResponse.success(
                "AUTH_ME",
                "Sesión actual",
                authService.me()
        );
    }
}
