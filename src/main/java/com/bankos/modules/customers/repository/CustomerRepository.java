package com.bankos.modules.customers.repository;

import com.bankos.modules.customers.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Customer> findAllByTenantId(UUID tenantId, Pageable pageable);

    boolean existsByTenantIdAndDocumentNumber(UUID tenantId, String documentNumber);
}
