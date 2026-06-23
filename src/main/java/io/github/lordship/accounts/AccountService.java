package io.github.lordship.accounts;

import io.github.lordship.accounts.internal.AccountRepository;
import io.github.lordship.accounts.internal.AccountRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account createAccount(UUID tenancyId, String notes) {
        AccountRow row = new AccountRow(tenancyId, notes);
        return accountRepository.save(row).toAccount();
    }

    public Optional<Account> getAccount(UUID uuid) {
        return accountRepository.findById(uuid).map(AccountRow::toAccount);
    }

    public List<Account> getAccountsByProperty(UUID propertyId) {
        return accountRepository.findActiveByPropertyId(propertyId)
                .stream()
                .map(AccountRow::toAccount)
                .toList();
    }

    @Transactional
    public Optional<Account> updateAccount(UUID uuid, AccountStatus status, BigDecimal balance, boolean autopayEnabled, String notes) {
        return accountRepository.update(uuid, status.name(), balance, autopayEnabled, notes)
                .map(AccountRow::toAccount);
    }

    @Transactional
    public Optional<Account> deactivateAccount(UUID uuid) {
        return accountRepository.softDelete(uuid).map(AccountRow::toAccount);
    }
}
