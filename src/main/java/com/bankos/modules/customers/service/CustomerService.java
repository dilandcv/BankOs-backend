package com.bankos.modules.customers.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.response.PageResponse;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.customers.dto.CreateCustomerRequest;
import com.bankos.modules.customers.dto.CustomerResponse;
import com.bankos.modules.customers.entity.Customer;
import com.bankos.modules.customers.entity.CustomerStatus;
import com.bankos.modules.customers.mapper.CustomerMapper;
import com.bankos.modules.customers.repository.CustomerRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository, AuditService auditService) {
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorUserId = TenantContext.requireUserId();
        String documentNumber = request.documentNumber().trim();

        if (customerRepository.existsByTenantIdAndDocumentNumber(tenantId, documentNumber)) {
            throw new ApiException(ErrorCode.CONFLICT, "Ya existe un cliente con ese documento en la institución");
        }

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setDocumentType(request.documentType().trim().toUpperCase());
        customer.setDocumentNumber(documentNumber);
        customer.setFullName(request.fullName().trim());
        customer.setStatus(CustomerStatus.ACTIVE);

        Customer saved = customerRepository.save(customer);
        auditService.record(
                tenantId,
                actorUserId,
                "CUSTOMER_CREATED",
                "CUSTOMER",
                saved.getId(),
                Map.of("document_number", saved.getDocumentNumber(), "full_name", saved.getFullName())
        );
        return CustomerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return PageResponse.from(customerRepository.findAllByTenantId(tenantId, pageable)
                .map(CustomerMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.CUSTOMER_NOT_FOUND));
        return CustomerMapper.toResponse(customer);
    }
}
