package com.bankos.modules.customers.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.common.response.PageResponse;
import com.bankos.modules.customers.dto.CreateCustomerRequest;
import com.bankos.modules.customers.dto.CustomerResponse;
import com.bankos.modules.customers.service.CustomerService;
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
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'OPERATOR')")
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        return ApiResponse.success(
                "CUSTOMER_CREATED",
                "Cliente creado correctamente",
                customerService.create(request)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<PageResponse<CustomerResponse>> list(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(
                "CUSTOMERS_LISTED",
                "Clientes de la institución",
                customerService.list(pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<CustomerResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(
                "CUSTOMER_FOUND",
                "Cliente encontrado",
                customerService.get(id)
        );
    }
}
