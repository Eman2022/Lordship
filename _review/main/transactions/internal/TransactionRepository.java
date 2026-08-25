package io.github.lordship.transactions.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRepository {

    private final JdbcClient jdbc;

    public TransactionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public TransactionRow save(TransactionRow row) {
        return jdbc.sql("""
                INSERT INTO transaction (account_id, type, amount, description, billing_period)
                VALUES (:accountId, :type, :amount, :description, :billingPeriod)
                RETURNING *
                """)
                .paramSource(row)
                .query(TransactionRow.class)
                .single();
    }

    public Optional<TransactionRow> findById(UUID uuid) {
        return jdbc.sql("SELECT * FROM transaction WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(TransactionRow.class)
                .optional();
    }

    public List<TransactionRow> findByAccountId(UUID accountId) {
        return jdbc.sql("SELECT * FROM transaction WHERE account_id = :accountId AND deleted_at IS NULL ORDER BY posted_at DESC")
                .param("accountId", accountId)
                .query(TransactionRow.class)
                .list();
    }

    public Optional<TransactionRow> softDelete(UUID uuid) {
        return jdbc.sql("""
                UPDATE transaction
                SET deleted_at = CURRENT_TIMESTAMP
                WHERE uuid = :uuid AND deleted_at IS NULL
                RETURNING *
                """)
                .param("uuid", uuid)
                .query(TransactionRow.class)
                .optional();
    }

    public void updateBalanceCached(UUID accountId, BigDecimal newBalance) {
        jdbc.sql("UPDATE account SET balance_cached = :newBalance WHERE uuid = :accountId AND deleted_at IS NULL")
                .param("newBalance", newBalance)
                .param("accountId", accountId)
                .update();
    }

    // Use sparingly — full ledger scan across all transactions for the account.
    // For reads, prefer account.balanceCached(). Only call this after a mutation
    // (post or delete transaction) to refresh the cached value on the account.
    public BigDecimal computeBalance(UUID accountId) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(
                    CASE WHEN type IN ('CREDIT', 'PAYMENT')
                         THEN -amount
                         ELSE amount
                    END
                ), 0)
                FROM transaction
                WHERE account_id = :accountId AND deleted_at IS NULL
                """)
                .param("accountId", accountId)
                .query(BigDecimal.class)
                .single();
    }

    // Historical balance as of a specific date — sums only transactions whose
    // billing_period is strictly before asOfDate. Use for rent roll reconstruction
    // (e.g. "what was this account's balance at the end of March?").
    public BigDecimal computeBalance(UUID accountId, LocalDate asOfDate) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(
                    CASE WHEN type IN ('CREDIT', 'PAYMENT')
                         THEN -amount
                         ELSE amount
                    END
                ), 0)
                FROM transaction
                WHERE account_id = :accountId
                  AND billing_period < :asOfDate
                  AND deleted_at IS NULL
                """)
                .param("accountId", accountId)
                .param("asOfDate", asOfDate)
                .query(BigDecimal.class)
                .single();
    }
}
