package io.github.lordship.lots.internal;

import io.github.lordship.shared.AgreementType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class LotPermissibleAgreementTypeRepository {

    private final JdbcClient jdbc;
    private final LotPermissibleAgreementTypeRowMapper rowMapper = new LotPermissibleAgreementTypeRowMapper();

    public LotPermissibleAgreementTypeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public LotPermissibleAgreementTypeRow save(LotPermissibleAgreementTypeRow row) {
        return jdbc.sql("""
            INSERT INTO lot_permissible_agreement_type (lot_id, agreement_type, target_rent)
            VALUES (:lotId, :agreementType::agreement_type, :targetRent)
            ON CONFLICT (lot_id, agreement_type)
            DO UPDATE SET target_rent = EXCLUDED.target_rent
            RETURNING *
            """)
                .param("lotId", row.lotId())
                .param("agreementType", row.agreementType().name())
                .param("targetRent", row.targetRent())
                .query(rowMapper)
                .single();
    }

    public List<LotPermissibleAgreementTypeRow> findByLotId(UUID lotId) {
        return jdbc.sql("""
            SELECT * FROM lot_permissible_agreement_type
            WHERE lot_id = :lotId
            ORDER BY agreement_type
            """)
                .param("lotId", lotId)
                .query(rowMapper)
                .list();
    }

    public List<LotPermissibleAgreementTypeRow> findByLotIds(Collection<UUID> lotIds) {
        if (lotIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
            SELECT * FROM lot_permissible_agreement_type
            WHERE lot_id IN (:lotIds)
            ORDER BY lot_id, agreement_type
            """)
                .param("lotIds", lotIds)
                .query(rowMapper)
                .list();
    }

    public void delete(UUID lotId, AgreementType agreementType) {
        jdbc.sql("""
            DELETE FROM lot_permissible_agreement_type
            WHERE lot_id = :lotId AND agreement_type = :agreementType::agreement_type
            """)
                .param("lotId", lotId)
                .param("agreementType", agreementType.name())
                .update();
    }
}