package com.bankos.modules.users.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.common.response.PageResponse;
import com.bankos.modules.users.dto.CreateUserRequest;
import com.bankos.modules.users.dto.UserResponse;
import com.bankos.modules.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(
                "USER_CREATED",
                "Usuario creado correctamente",
                userService.create(request)
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(
                "USERS_LISTED",
                "Usuarios de la institución",
                userService.list(pageable)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(
                "USER_FOUND",
                "Usuario encontrado",
                userService.get(id)
        );
    }
}
