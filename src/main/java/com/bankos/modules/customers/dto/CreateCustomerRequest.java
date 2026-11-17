package com.bankos.modules.customers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank @Size(max = 30) String documentType,
        @NotBlank @Size(max = 50) String documentNumber,
        @NotBlank @Size(max = 150) String fullName
) {
}
