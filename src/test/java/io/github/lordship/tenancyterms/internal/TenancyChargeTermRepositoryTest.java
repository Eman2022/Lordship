package io.github.lordship.tenancyterms.internal;

import io.github.lordship.IntegrationTest;
import io.github.lordship.lots.internal.LotRow;
import io.github.lordship.properties.internal.PropertyRow;
import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.FeeMethod;
import io.github.lordship.shared.SystemPrincipal;
import io.github.lordship.shared.UtilityMethod;
import io.github.lordship.tenancy.internal.TenancyRow;
import io.github.lordship.tenancyterms.TenancyTermSource;
import io.github.lordship.tenancyterms.TenancyTermStatus;
import io.github.lordship.termstemplate.TermsTemplate;
import io.github.lordship.termstemplate.internal.TermsTemplateRepository;
import io.github.lordship.termstemplate.internal.TermsTemplateRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class TenancyChargeTermRepositoryTest extends IntegrationTest {

    @Autowired
    TenancyChargeTermRepository tenancyChargeTermRepository;

    @Autowired
    TermsTemplateRepository termsTemplateRepository;

    @Autowired
    JdbcClient jdbc;

    private UUID tenancy;
    private UUID property;
    private TermsTemplate template;

    @BeforeEach
    void setUp() {
        PropertyRow propertyRow = testData.insertProperty("CT");
        LotRow lot = testData.insertLot(propertyRow.uuid(), "1");
        TenancyRow tenancyRow = testData.insertTenancy(lot.uuid());

        property = propertyRow.uuid();
        tenancy = tenancyRow.uuid();
        template = templateFor(property, "CT Land Lease");
    }

    // ---- save and the row mapper --------------------------------------------

    @Test
    void save_shouldFillInTheDatabaseGeneratedColumns() {
        // Act
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));

        // Assert
        assertThat(saved.uuid()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.deletedAt()).isNull();
        assertThat(saved.status()).isEqualTo(TenancyTermStatus.PROPOSED);
        assertThat(saved.termsTemplate()).isEqualTo(template.uuid());
    }

    // Reading a column that does not exist throws; reading the WRONG column
    // silently returns someone else's value. Nine money columns sit next to each
    // other in this table, so every one gets a value nothing else shares.
    @Test
    void rowMapper_shouldPopulateEveryColumn_onARoundTrip() {
        // Arrange -- the pairs are deliberately inconsistent, which a PROPOSED
        // row is allowed to be. That is what lets each column carry a value
        // distinct from every other one.
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(distinctRow());

        // Act
        TenancyChargeTermRow read = tenancyChargeTermRepository.findById(saved.uuid()).orElseThrow();

        // Assert
        assertThat(read.tenancy()).isEqualTo(tenancy);
        assertThat(read.validAt()).isEqualTo(LocalDate.of(2027, 3, 4));
        assertThat(read.agreementType()).isEqualTo(AgreementType.LAND);

        assertThat(read.rate()).isEqualByComparingTo("101.00");
        assertThat(read.carFee()).isEqualByComparingTo("102.00");
        assertThat(read.allowedCars()).isEqualTo(3);
        assertThat(read.carsMax()).isEqualTo(7);
        assertThat(read.petFee()).isEqualByComparingTo("103.00");
        assertThat(read.allowedPets()).isEqualTo(5);

        assertThat(read.paymentDueDay()).isEqualTo(9);
        assertThat(read.gracePeriodDays()).isEqualTo(11);

        assertThat(read.ruleViolationFeeMethod()).isEqualTo(FeeMethod.FLAT);
        assertThat(read.ruleViolationFeeAmount()).isEqualByComparingTo("201.00");
        assertThat(read.nsfFeeMethod()).isEqualTo(FeeMethod.BANK_OR_FLAT);
        assertThat(read.nsfFeeAmount()).isEqualByComparingTo("202.00");
        assertThat(read.lateFeeMethod()).isEqualTo(FeeMethod.PERCENT_OF_RENT);
        assertThat(read.lateFeeAmount()).isEqualByComparingTo("203.00");

        assertThat(read.waterMethod()).isEqualTo(UtilityMethod.FLAT);
        assertThat(read.waterFlatAmount()).isEqualByComparingTo("301.00");
        assertThat(read.powerMethod()).isEqualTo(UtilityMethod.RUBS);
        assertThat(read.powerFlatAmount()).isEqualByComparingTo("302.00");
        assertThat(read.sewerMethod()).isEqualTo(UtilityMethod.SUBMETERED);
        assertThat(read.sewerFlatAmount()).isEqualByComparingTo("303.00");
        assertThat(read.trashMethod()).isEqualTo(UtilityMethod.RUBS);
        assertThat(read.trashFlatAmount()).isEqualByComparingTo("304.00");

        assertThat(read.status()).isEqualTo(TenancyTermStatus.PROPOSED);
        assertThat(read.source()).isEqualTo(TenancyTermSource.CORRECTION);
        assertThat(read.termsTemplate()).isEqualTo(template.uuid());
        assertThat(read.note()).isEqualTo("CT round trip");
        assertThat(read.createdBy()).isEqualTo(SystemPrincipal.AGENT_UUID);
    }

    @Test
    void findById_shouldNotReturnASoftDeletedTerm() {
        // Arrange
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));
        tenancyChargeTermRepository.softDelete(saved.uuid());

        // Act / Assert
        assertThat(tenancyChargeTermRepository.findById(saved.uuid())).isEmpty();
    }

    @Test
    void findByTenancy_shouldReturnNewestFirst() {
        // Arrange
        tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));
        tenancyChargeTermRepository.save(draft(LocalDate.of(2027, 1, 1)));
        tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 11, 1)));

        // Act
        List<TenancyChargeTermRow> found = tenancyChargeTermRepository.findByTenancy(tenancy);

        // Assert
        assertThat(found).extracting(TenancyChargeTermRow::validAt).containsExactly(
                LocalDate.of(2027, 1, 1), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 9, 1));
    }

    @Test
    void findByBatch_shouldReturnOnlyThatRun() {
        // Arrange -- a bulk run has to be reviewable and abandonable on its own
        UUID batch = UUID.randomUUID();
        tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1), batch));
        tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 10, 1), batch));
        tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 11, 1), UUID.randomUUID()));
        tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 12, 1)));

        // Act / Assert
        assertThat(tenancyChargeTermRepository.findByBatch(batch)).hasSize(2);
    }

    // ---- patch ---------------------------------------------------------------

    @Test
    void patch_shouldRejectAColumnOutsideTheWhitelist() {
        // Arrange -- changing the agreement type would move the tenancy to a
        // different statute without anybody signing anything
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));

        // Act / Assert
        assertThatThrownBy(() -> tenancyChargeTermRepository.patch(
                saved.uuid(), Map.of("agreement_type", "STORAGE")))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
    }

    @Test
    void patch_shouldUpdateAndReturnTheRow() {
        // Arrange
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));

        // Act
        TenancyChargeTermRow patched = tenancyChargeTermRepository.patch(
                saved.uuid(), Map.of("rate", new BigDecimal("777.00"), "note", "raised")).orElseThrow();

        // Assert
        assertThat(patched.rate()).isEqualByComparingTo("777.00");
        assertThat(patched.note()).isEqualTo("raised");
    }

    // ---- the escaped CHECK constraints --------------------------------------

    @Test
    void updateStatus_shouldMoveAConsistentDraftToPending() {
        // Arrange
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));

        // Act
        Optional<TenancyChargeTermRow> moved = tenancyChargeTermRepository.updateStatus(
                saved.uuid(), TenancyTermStatus.PROPOSED, TenancyTermStatus.PENDING);

        // Assert
        assertThat(moved).isPresent();
        assertThat(moved.get().status()).isEqualTo(TenancyTermStatus.PENDING);
    }

    @Test
    void updateStatus_shouldReturnEmpty_whenTheTermIsNotInTheExpectedState() {
        // Arrange -- the guard that stops two agents submitting the same term
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));

        // Act / Assert
        assertThat(tenancyChargeTermRepository.updateStatus(
                saved.uuid(), TenancyTermStatus.PENDING, TenancyTermStatus.ACTIVE)).isEmpty();
    }

    // This is the whole reason the service validates before submitting. The
    // amount/method CHECKs all read "status = 'PROPOSED' OR ...", so Postgres
    // re-evaluates them on the UPDATE that changes status -- not on the insert.
    @Test
    void updateStatus_shouldRaiseAnAmountConstraint_onLeavingProposed() {
        // Arrange -- a FLAT late fee with no amount: legal to save, illegal to submit
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));
        tenancyChargeTermRepository.patch(saved.uuid(), Map.of(
                "late_fee_method", "FLAT",
                "late_fee_amount", BigDecimal.ZERO));

        // Act / Assert
        assertThatThrownBy(() -> tenancyChargeTermRepository.updateStatus(
                saved.uuid(), TenancyTermStatus.PROPOSED, TenancyTermStatus.PENDING))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updateStatus_shouldRaiseTheCarsConstraint_onLeavingProposed() {
        // Arrange
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));
        tenancyChargeTermRepository.patch(saved.uuid(), Map.of("allowed_cars", 6, "cars_max", 2));

        // Act / Assert
        assertThatThrownBy(() -> tenancyChargeTermRepository.updateStatus(
                saved.uuid(), TenancyTermStatus.PROPOSED, TenancyTermStatus.PENDING))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updateStatus_shouldRefuseToActivateATermWithNoPaperBehindIt() {
        // Arrange -- term_in_force_needs_paper; source is LEASE, not MIGRATION
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));
        tenancyChargeTermRepository.updateStatus(
                saved.uuid(), TenancyTermStatus.PROPOSED, TenancyTermStatus.PENDING);

        // Act / Assert
        assertThatThrownBy(() -> tenancyChargeTermRepository.updateStatus(
                saved.uuid(), TenancyTermStatus.PENDING, TenancyTermStatus.ACTIVE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---- cancel --------------------------------------------------------------

    @Test
    void cancel_shouldSetAllFourColumnsAtOnce() {
        // Arrange -- term_cancel_facts and term_cancel_fields_only_when_cancelled
        // point in opposite directions, so a partial write fails one of them
        TenancyChargeTermRow active = activeTermAt(LocalDate.of(2026, 9, 1));

        // Act
        TenancyChargeTermRow cancelled = tenancyChargeTermRepository.cancel(
                active.uuid(), SystemPrincipal.AGENT_UUID, "tenant moved out").orElseThrow();

        // Assert
        assertThat(cancelled.status()).isEqualTo(TenancyTermStatus.CANCELLED);
        assertThat(cancelled.cancelledAt()).isNotNull();
        assertThat(cancelled.cancelledBy()).isEqualTo(SystemPrincipal.AGENT_UUID);
        assertThat(cancelled.cancelReason()).isEqualTo("tenant moved out");
    }

    @Test
    void cancel_shouldReturnEmpty_whenTheTermWasNeverInForce() {
        // Arrange -- a draft is deleted, not cancelled
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));

        // Act / Assert
        assertThat(tenancyChargeTermRepository.cancel(
                saved.uuid(), SystemPrincipal.AGENT_UUID, "nope")).isEmpty();
    }

    // ---- soft delete ---------------------------------------------------------

    @Test
    void softDelete_shouldRemoveADraft() {
        // Arrange
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));

        // Act / Assert
        assertThat(tenancyChargeTermRepository.softDelete(saved.uuid())).isTrue();
    }

    // The status guard lives in the WHERE clause, so this answers false rather
    // than raising term_delete_only_before_force -- which is what keeps the
    // service's "only audit when a row actually changed" rule working.
    @Test
    void softDelete_shouldReturnFalse_forATermInForce() {
        // Arrange
        TenancyChargeTermRow active = activeTermAt(LocalDate.of(2026, 9, 1));

        // Act / Assert
        assertThat(tenancyChargeTermRepository.softDelete(active.uuid())).isFalse();
        assertThat(tenancyChargeTermRepository.findById(active.uuid())).isPresent();
    }

    // ---- findInForceOn: what billing asks ------------------------------------

    @Test
    void findInForceOn_shouldPickTheLatestTermTakingEffectOnOrBeforeTheDate() {
        // Arrange
        activeTermAt(LocalDate.of(2026, 1, 1));
        TenancyChargeTermRow current = activeTermAt(LocalDate.of(2026, 7, 1));

        // Act
        Optional<TenancyChargeTermRow> found =
                tenancyChargeTermRepository.findInForceOn(tenancy, LocalDate.of(2026, 9, 1));

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().uuid()).isEqualTo(current.uuid());
    }

    @Test
    void findInForceOn_shouldIgnoreATermThatTakesEffectLater() {
        // Arrange -- a rent increase served in advance must not bill early
        TenancyChargeTermRow current = activeTermAt(LocalDate.of(2026, 1, 1));
        activeTermAt(LocalDate.of(2026, 10, 1));

        // Act
        Optional<TenancyChargeTermRow> found =
                tenancyChargeTermRepository.findInForceOn(tenancy, LocalDate.of(2026, 9, 1));

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().uuid()).isEqualTo(current.uuid());
    }

    @Test
    void findInForceOn_shouldIgnoreACancelledTerm() {
        // Arrange -- cancelled leaves resolution entirely
        TenancyChargeTermRow superseded = activeTermAt(LocalDate.of(2026, 1, 1));
        TenancyChargeTermRow retracted = activeTermAt(LocalDate.of(2026, 7, 1));
        tenancyChargeTermRepository.cancel(retracted.uuid(), SystemPrincipal.AGENT_UUID, "retracted");

        // Act
        Optional<TenancyChargeTermRow> found =
                tenancyChargeTermRepository.findInForceOn(tenancy, LocalDate.of(2026, 9, 1));

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().uuid()).isEqualTo(superseded.uuid());
    }

    @Test
    void findInForceOn_shouldIgnoreATermStillOutForSignature() {
        // Arrange -- PENDING is not in force
        TenancyChargeTermRow pending = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 1, 1)));
        tenancyChargeTermRepository.updateStatus(
                pending.uuid(), TenancyTermStatus.PROPOSED, TenancyTermStatus.PENDING);

        // Act / Assert
        assertThat(tenancyChargeTermRepository.findInForceOn(tenancy, LocalDate.of(2026, 9, 1))).isEmpty();
    }

    @Test
    void twoTermsCannotTakeEffectForTheSameTenancyOnTheSameDate() {
        // Arrange -- tenancy_charge_term_in_force_uq is what makes the resolver
        // deterministic rather than "whichever row Postgres reaches first"
        activeTermAt(LocalDate.of(2026, 9, 1));

        // Act / Assert
        assertThatThrownBy(() -> activeTermAt(LocalDate.of(2026, 9, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---- attachSource: the composite foreign key -----------------------------

    @Test
    void attachSource_shouldAcceptAnInstrumentFromTheSameTenancy() {
        // Arrange
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));
        UUID instrument = insertInstrument(tenancy);

        // Act
        TenancyChargeTermRow attached =
                tenancyChargeTermRepository.attachSource(saved.uuid(), instrument).orElseThrow();

        // Assert
        assertThat(attached.sourceUuid()).isEqualTo(instrument);
    }

    // The composite FK to instrument(uuid, tenancy) is the only thing standing
    // between a signed lease and the wrong tenant's file.
    @Test
    void attachSource_shouldRejectAnInstrumentFromAnotherTenancy() {
        // Arrange
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(draft(LocalDate.of(2026, 9, 1)));
        LotRow otherLot = testData.insertLot(property, "2");
        UUID otherTenancy = testData.insertTenancy(otherLot.uuid()).uuid();
        UUID foreignInstrument = insertInstrument(otherTenancy);

        // Act / Assert
        assertThatThrownBy(() -> tenancyChargeTermRepository.attachSource(saved.uuid(), foreignInstrument))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---- Fixtures ------------------------------------------------------------

    /**
     * A property-level template, inserted straight through the repository.
     * Deliberately not via TermsTemplateService: a repository test has no
     * principal, and going through a service would pull in the audit write,
     * which is meant to refuse an unattributed row rather than invent one.
     */
    private TermsTemplate templateFor(UUID property, String name) {
        return termsTemplateRepository.save(new TermsTemplateRow(
                property, name, AgreementType.LAND, SystemPrincipal.AGENT_UUID)).toTermsTemplate();
    }

    /** A consistent draft copied from the template, as the service would build it. */
    private TenancyChargeTermRow draft(LocalDate validAt) {
        return draft(validAt, null);
    }

    private TenancyChargeTermRow draft(LocalDate validAt, UUID batch) {
        return TenancyChargeTermRow.fromTemplate(
                tenancy, template, new BigDecimal("650.00"), validAt,
                TenancyTermSource.LEASE, batch, SystemPrincipal.AGENT_UUID);
    }

    /**
     * A term in force. Sourced MIGRATION because that is the one value
     * term_in_force_needs_paper exempts, which keeps the fixture from needing a
     * whole instrument behind it.
     */
    private TenancyChargeTermRow activeTermAt(LocalDate validAt) {
        TenancyChargeTermRow saved = tenancyChargeTermRepository.save(
                TenancyChargeTermRow.fromTemplate(
                        tenancy, template, new BigDecimal("650.00"), validAt,
                        TenancyTermSource.MIGRATION, null, SystemPrincipal.AGENT_UUID));

        tenancyChargeTermRepository.updateStatus(
                saved.uuid(), TenancyTermStatus.PROPOSED, TenancyTermStatus.PENDING).orElseThrow();
        return tenancyChargeTermRepository.updateStatus(
                saved.uuid(), TenancyTermStatus.PENDING, TenancyTermStatus.ACTIVE).orElseThrow();
    }

    private UUID insertInstrument(UUID forTenancy) {
        return jdbc.sql("""
                INSERT INTO instrument (tenancy, type, created_by)
                VALUES (:tenancy, :type::instrument_type, :createdBy)
                RETURNING uuid
                """)
                .param("tenancy", forTenancy)
                .param("type", "LEASE")
                .param("createdBy", SystemPrincipal.AGENT_UUID)
                .query(UUID.class)
                .single();
    }

    /**
     * Every column set to a value nothing else in the row shares, so a
     * transposed pair in the row mapper cannot hide. The method/amount pairs are
     * deliberately mismatched -- a PROPOSED row is allowed to be inconsistent,
     * and that is what frees each amount to be unique.
     */
    private TenancyChargeTermRow distinctRow() {
        return new TenancyChargeTermRow(
                null,
                tenancy,
                LocalDate.of(2027, 3, 4),
                AgreementType.LAND,
                new BigDecimal("101.00"),
                new BigDecimal("102.00"),
                3,
                7,
                new BigDecimal("103.00"),
                5,
                9,
                11,
                FeeMethod.FLAT, new BigDecimal("201.00"),
                FeeMethod.BANK_OR_FLAT, new BigDecimal("202.00"),
                FeeMethod.PERCENT_OF_RENT, new BigDecimal("203.00"),
                UtilityMethod.FLAT, new BigDecimal("301.00"),
                UtilityMethod.RUBS, new BigDecimal("302.00"),
                UtilityMethod.SUBMETERED, new BigDecimal("303.00"),
                UtilityMethod.RUBS, new BigDecimal("304.00"),
                TenancyTermStatus.PROPOSED,
                TenancyTermSource.CORRECTION,
                null,
                template.uuid(),
                null,
                null, null, null,
                null,
                "CT round trip",
                null,
                SystemPrincipal.AGENT_UUID);
    }
}
