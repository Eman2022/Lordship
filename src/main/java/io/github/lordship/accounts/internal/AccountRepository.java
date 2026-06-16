package io.github.lordship.accounts.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
                INSERT INTO account (tenancy_id, account_status, balance, autopay_enabled, notes)
                VALUES (:tenancyId, :accountStatus, :balance, :autopayEnabled, :notes)
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

    public List<AccountRow> findActiveByPropertyCode(String propertyCode) {
        return jdbc.sql("""
                SELECT a.* FROM account a
                JOIN tenancy t ON a.tenancy_id = t.uuid
                JOIN lot l ON t.lot_id = l.uuid
                WHERE l.property_code = :propertyCode
                AND a.deleted_at IS NULL
                """)
                .param("propertyCode", propertyCode)
                .query(AccountRow.class)
                .list();
    }

    public Optional<AccountRow> update(UUID uuid, String accountStatus, BigDecimal balance, boolean autopayEnabled, String notes) {
        return jdbc.sql("""
                UPDATE account
                SET account_status = :accountStatus,
                    balance        = :balance,
                    autopay_enabled = :autopayEnabled,
                    notes          = :notes
                WHERE uuid = :uuid AND deleted_at IS NULL
                RETURNING *
                """)
                .param("uuid", uuid)
                .param("accountStatus", accountStatus)
                .param("balance", balance)
                .param("autopayEnabled", autopayEnabled)
                .param("notes", notes)
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
