package io.github.lordship.transactions;

import io.github.lordship.accounts.Account;
import io.github.lordship.accounts.AccountService;
import io.github.lordship.transactions.internal.TransactionRepository;
import io.github.lordship.transactions.internal.TransactionRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransactionService(TransactionRepository transactionRepository, AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    @Transactional
    public Transaction postTransaction(UUID accountId, TransactionType type, BigDecimal amount,
                                       String description, LocalDate billingPeriod) {
        if ((type == TransactionType.CREDIT || type == TransactionType.BALANCE_ADJUSTMENT
                || type == TransactionType.MISC_CHARGE)
                && (description == null || description.isBlank())) {
            throw new IllegalArgumentException("Description is required for transaction type: " + type);
        }
        Account account = accountService.getAccount(accountId)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));
        if (type == TransactionType.PAYMENT && !account.acceptPayments()) {
            throw new IllegalStateException("Account does not accept payments: " + accountId);
        }
        TransactionRow row = new TransactionRow(accountId, type, amount, description, billingPeriod);
        Transaction tx = transactionRepository.save(row).toTransaction();
        log.info("Transaction posted: uuid={}, accountId={}, type={}, amount={}", tx.uuid(), tx.accountId(), tx.type(), tx.amount());
        return tx;
    }

    public Transaction findById(UUID uuid) {
        return transactionRepository.findById(uuid)
                .map(TransactionRow::toTransaction)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + uuid));
    }

    public List<Transaction> findByAccountId(UUID accountId) {
        return transactionRepository.findByAccountId(accountId)
                .stream()
                .map(TransactionRow::toTransaction)
                .toList();
    }

    @Transactional
    public void deleteTransaction(UUID uuid) {
        TransactionRow row = transactionRepository.findById(uuid)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + uuid));
        LocalDate closedBefore = LocalDate.now().withDayOfMonth(1);
        if (row.billingPeriod().isBefore(closedBefore)) {
            throw new IllegalStateException("Cannot delete a transaction from a closed billing period: " + uuid);
        }
        transactionRepository.softDelete(uuid);
        log.info("Transaction soft deleted: uuid={}", uuid);
    }

    public BigDecimal computeBalance(UUID accountId) {
        return transactionRepository.computeBalance(accountId);
    }
}
