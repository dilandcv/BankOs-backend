package com.bankos.modules.customers.mapper;

import com.bankos.modules.customers.dto.CustomerResponse;
import com.bankos.modules.customers.entity.Customer;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getDocumentType(),
                customer.getDocumentNumber(),
                customer.getFullName(),
                customer.getStatus().name(),
                customer.getCreatedAt()
        );
    }
}
