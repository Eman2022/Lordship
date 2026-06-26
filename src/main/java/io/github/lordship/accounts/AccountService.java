package io.github.lordship.accounts;

import io.github.lordship.accounts.internal.AccountRepository;
import io.github.lordship.accounts.internal.AccountRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Optional<Account> getAccountByTenancyId(UUID tenancyId) {
        return accountRepository.findByTenancyId(tenancyId).map(AccountRow::toAccount);
    }

    public List<Account> getAccountsByProperty(UUID propertyId) {
        return accountRepository.findActiveByPropertyId(propertyId)
                .stream()
                .map(AccountRow::toAccount)
                .toList();
    }

    @Transactional
    public Optional<Account> updateAccount(Account account) {
        AccountRow row = new AccountRow(
                account.uuid(),
                account.tenancyId(),
                account.accountStatus().name(),
                account.balance(),
                account.autopayEnabled(),
                account.notes(),
                account.noPersonalChecks(),
                account.noPartialPayments(),
                account.evictionInProgress(),
                account.createdAt(),
                account.deletedAt()
        );
        return accountRepository.update(row).map(AccountRow::toAccount);
    }

    @Transactional
    public Optional<Account> deactivateAccount(UUID uuid) {
        return accountRepository.softDelete(uuid).map(AccountRow::toAccount);
    }
}
