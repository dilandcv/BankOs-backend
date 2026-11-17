package com.bankos.modules.accounts.mapper;

import com.bankos.modules.accounts.dto.AccountResponse;
import com.bankos.modules.accounts.entity.Account;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }
}
