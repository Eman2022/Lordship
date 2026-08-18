package io.github.lordship.accounts;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.accounts.internal.AccountRepository;
import io.github.lordship.accounts.internal.AccountRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AuditService auditService;

    public AccountService(AccountRepository accountRepository, AuditService auditService) {
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Account createAccount(UUID tenancyId, String notes) {
        AccountRow row = new AccountRow(tenancyId, notes);
        AccountRow saved = accountRepository.save(row);
        auditService.recordInsert("account", saved.uuid(), AuditMapper.toMap(saved));
        log.info("Account created: uuid={}, tenancyId={}", saved.uuid(), saved.tenancyId());
        return saved.toAccount();
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
        AccountRow before = accountRepository.findById(account.uuid()).orElse(null);
        AccountRow row = new AccountRow(
                account.uuid(),
                account.tenancyId(),
                account.accountStatus().name(),
                account.balanceCached(),
                account.autopayEnabled(),
                account.notes(),
                account.createdAt(),
                account.deletedAt()
        );
        Optional<AccountRow> result = accountRepository.update(row);
        result.ifPresent(after -> {
            if (before != null) {
                // diff.before() holds the old values of only the fields that changed.
                // diff.after() holds the new values for those same fields.
                // isEmpty() guard prevents recording a no-op audit entry when nothing actually changed.
                AuditMapper.Diff diff = AuditMapper.diff(before, after);
                if (!diff.before().isEmpty()) {
                    log.info("Account updated uuid={}: changed fields={}", account.uuid(), diff.before().keySet());
                    auditService.recordUpdate("account", account.uuid(), diff.before(), diff.after());
                }
            }
        });
        return result.map(AccountRow::toAccount);
    }

    @Transactional
    public Optional<Account> patchAccount(UUID uuid, Map<String, Object> changes) {
        Optional<AccountRow> beforeOpt = accountRepository.findById(uuid);
        if (beforeOpt.isEmpty()) {
            return Optional.empty();
        }
        AccountRow before = beforeOpt.get();

        Optional<AccountRow> afterOpt = accountRepository.patch(uuid, changes);
        if (afterOpt.isEmpty()) {
            return Optional.empty();
        }
        AccountRow after = afterOpt.get();

        AuditMapper.Diff diff = AuditMapper.diff(before, after);
        if (!diff.before().isEmpty()) {
            log.info("Account patched uuid={}: changed fields={}", uuid, diff.before().keySet());
            auditService.recordUpdate("account", uuid, diff.before(), diff.after());
        }
        return Optional.of(after.toAccount());
    }

    @Transactional
    public Optional<Account> deactivateAccount(UUID uuid) {
        return accountRepository.findById(uuid).flatMap(before -> {
            Optional<AccountRow> result = accountRepository.softDelete(uuid);
            result.ifPresent(deleted -> {
                auditService.recordDelete("account", uuid, AuditMapper.toMap(before));
                log.info("Account soft deleted: uuid={}", uuid);
            });
            return result.map(AccountRow::toAccount);
        });
    }
}
