package io.github.lordship.transactions;

import io.github.lordship.transactions.internal.TransactionRepository;
import io.github.lordship.transactions.internal.TransactionRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction postTransaction(UUID accountId, TransactionType type, BigDecimal amount,
                                       String description, LocalDate billingPeriod) {
        TransactionRow row = new TransactionRow(accountId, type, amount, description, billingPeriod);
        return transactionRepository.save(row).toTransaction();
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
        if (row.billed()) {
            throw new IllegalStateException("Cannot delete a billed transaction: " + uuid);
        }
        transactionRepository.softDelete(uuid);
    }

    public BigDecimal computeBalance(UUID accountId) {
        return transactionRepository.computeBalance(accountId);
    }
}
