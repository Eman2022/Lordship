package io.github.lordship.accounts.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountRepository {

    private final JdbcClient jdbc;

    public AccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public AccountRow save(AccountRow row) {
        return jdbc.sql("""
                INSERT INTO account (tenancy_id, account_status, balance_cached, autopay_enabled, notes,
                                     no_personal_checks, no_partial_payments, accept_payments, exempt_from_late_fees)
                VALUES (:tenancyId, :accountStatus, :balanceCached, :autopayEnabled, :notes,
                        :noPersonalChecks, :noPartialPayments, :acceptPayments, :exemptFromLateFees)
                RETURNING *
                """)
                .paramSource(row)
                .query(AccountRow.class)
                .single();
    }

    public Optional<AccountRow> findById(UUID uuid) {
        return jdbc.sql("SELECT * FROM account WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(AccountRow.class)
                .optional();
    }

    public Optional<AccountRow> findByTenancyId(UUID tenancyId) {
        return jdbc.sql("SELECT * FROM account WHERE tenancy_id = :tenancyId AND deleted_at IS NULL")
                .param("tenancyId", tenancyId)
                .query(AccountRow.class)
                .optional();
    }

    public List<AccountRow> findActiveByPropertyId(UUID propertyId) {
        return jdbc.sql("""
                SELECT a.* FROM account a
                JOIN tenancy t ON a.tenancy_id = t.uuid
                JOIN lot l ON t.lot_id = l.uuid
                WHERE l.property_id = :propertyId
                AND a.deleted_at IS NULL
                """)
                .param("propertyId", propertyId)
                .query(AccountRow.class)
                .list();
    }

    public Optional<AccountRow> update(AccountRow row) {
        return jdbc.sql("""
                UPDATE account
                SET account_status       = :accountStatus,
                    autopay_enabled      = :autopayEnabled,
                    notes                = :notes,
                    no_personal_checks   = :noPersonalChecks,
                    no_partial_payments  = :noPartialPayments,
                    accept_payments      = :acceptPayments,
                    exempt_from_late_fees = :exemptFromLateFees
                WHERE uuid = :uuid AND deleted_at IS NULL
                RETURNING *
                """)
                .paramSource(row)
                .query(AccountRow.class)
                .optional();
    }

    public Optional<AccountRow> softDelete(UUID uuid) {
        return jdbc.sql("""
                UPDATE account
                SET deleted_at = CURRENT_TIMESTAMP
                WHERE uuid = :uuid AND deleted_at IS NULL
                RETURNING *
                """)
                .param("uuid", uuid)
                .query(AccountRow.class)
                .optional();
    }
}
