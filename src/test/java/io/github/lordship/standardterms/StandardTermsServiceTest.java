package io.github.lordship.standardterms;

import io.github.lordship.audit.AuditContext;
import io.github.lordship.audit.AuditService;
import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.FeeMethod;
import io.github.lordship.shared.SystemPrincipal;
import io.github.lordship.shared.UtilityMethod;
import io.github.lordship.standardterms.internal.StandardTermsRepository;
import io.github.lordship.standardterms.internal.StandardTermsRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StandardTermsServiceTest {

    @Mock
    StandardTermsRepository standardTermsRepository;

    @Mock
    AuditService auditService;

    @Mock
    AuditContext auditContext;

    @InjectMocks
    StandardTermsService standardTermsService;

    // ── Global templates ─────────────────────────────────────────────────────

    @Test
    void createGlobalTemplate_shouldSaveAndRecordAudit() {
        // Arrange
        StandardTermsRow stubRow = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findGlobalByName("WA Land Lease 2026")).thenReturn(Optional.empty());
        when(standardTermsRepository.save(any())).thenReturn(stubRow);

        // Act
        StandardTerms result = standardTermsService.createGlobalTemplate("WA Land Lease 2026", AgreementType.LAND);

        // Assert
        assertEquals("WA Land Lease 2026", result.name());
        assertNull(result.property(), "a global template has no property");
        verify(auditService).recordInsert(eq("standard_terms"), eq(stubRow.uuid()), any());
    }

    @Test
    void createGlobalTemplate_shouldAttributeToSystem_whenThereIsNoRequest() {
        // Arrange -- a unit test has no bound request, so ActingAgent falls back
        StandardTermsRow stubRow = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findGlobalByName(any())).thenReturn(Optional.empty());
        when(standardTermsRepository.save(any())).thenReturn(stubRow);

        // Act
        standardTermsService.createGlobalTemplate("WA Land Lease 2026", AgreementType.LAND);

        // Assert
        verify(standardTermsRepository).save(argThat(
                row -> SystemPrincipal.AGENT_UUID.equals(row.createdBy())));
    }

    @Test
    void createGlobalTemplate_shouldThrow_whenNameAlreadyTaken() {
        // Arrange
        StandardTermsRow existing = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findGlobalByName("WA Land Lease 2026")).thenReturn(Optional.of(existing));

        // Act / Assert
        assertThrows(IllegalStateException.class,
                () -> standardTermsService.createGlobalTemplate("WA Land Lease 2026", AgreementType.LAND));
        verify(standardTermsRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    // ── Copying a template into a property ───────────────────────────────────

    @Test
    void copyTemplateToProperty_shouldReturnEmpty_whenTemplateNotFound() {
        // Arrange
        UUID unknownUUID = UUID.randomUUID();
        when(standardTermsRepository.findById(unknownUUID)).thenReturn(Optional.empty());

        // Act
        Optional<StandardTerms> result =
                standardTermsService.copyTemplateToProperty(unknownUUID, UUID.randomUUID());

        // Assert
        assertTrue(result.isEmpty());
        verify(standardTermsRepository, never()).saveCopy(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void copyTemplateToProperty_shouldThrow_whenSourceIsAlreadyPropertyLevel() {
        // Arrange -- only a global template may be copied in
        UUID sourceProperty = UUID.randomUUID();
        StandardTermsRow propertyLevel =
                template(UUID.randomUUID(), "Copied Set", AgreementType.LAND).copyTo(sourceProperty, null);
        when(standardTermsRepository.findById(any())).thenReturn(Optional.of(propertyLevel));

        // Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> standardTermsService.copyTemplateToProperty(UUID.randomUUID(), UUID.randomUUID()));
        verify(standardTermsRepository, never()).saveCopy(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void copyTemplateToProperty_shouldThrow_whenPropertyAlreadyHasThatAgreementType() {
        // Arrange -- a property may hold at most one set per agreement type
        UUID property = UUID.randomUUID();
        StandardTermsRow templateRow = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(templateRow.uuid())).thenReturn(Optional.of(templateRow));
        when(standardTermsRepository.findByPropertyAndAgreementType(property, AgreementType.LAND))
                .thenReturn(Optional.of(templateRow.copyTo(property, null)));

        // Act / Assert
        assertThrows(IllegalStateException.class,
                () -> standardTermsService.copyTemplateToProperty(templateRow.uuid(), property));
        verify(standardTermsRepository, never()).saveCopy(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void copyTemplateToProperty_shouldCarryEveryTermValue_andRecordProvenance() {
        // Arrange
        UUID property = UUID.randomUUID();
        StandardTermsRow templateRow = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(templateRow.uuid())).thenReturn(Optional.of(templateRow));
        when(standardTermsRepository.findByPropertyAndAgreementType(property, AgreementType.LAND))
                .thenReturn(Optional.empty());
        when(standardTermsRepository.saveCopy(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        standardTermsService.copyTemplateToProperty(templateRow.uuid(), property);

        // Assert -- the copy must land on the property with its identity dropped,
        // its provenance recorded, and every term value intact.
        verify(standardTermsRepository).saveCopy(argThat(copy -> {
            assertNull(copy.uuid(), "the copy gets a new identity from the database");
            assertEquals(property, copy.property());
            assertEquals(templateRow.uuid(), copy.copiedFrom(), "provenance link back to the template");
            assertEquals(templateRow.name(), copy.name());
            assertEquals(templateRow.agreementType(), copy.agreementType());
            assertEquals(templateRow.targetRate(), copy.targetRate());
            assertEquals(templateRow.carFee(), copy.carFee());
            assertEquals(templateRow.allowedCars(), copy.allowedCars());
            assertEquals(templateRow.carsMax(), copy.carsMax()); // the column saveCopy used to drop
            assertEquals(templateRow.petFee(), copy.petFee());
            assertEquals(templateRow.allowedPets(), copy.allowedPets());
            assertEquals(templateRow.paymentDueDay(), copy.paymentDueDay());
            assertEquals(templateRow.gracePeriodDays(), copy.gracePeriodDays());
            assertEquals(templateRow.lateFeeMethod(), copy.lateFeeMethod());
            assertEquals(templateRow.lateFeeAmount(), copy.lateFeeAmount());
            assertEquals(templateRow.nsfFeeMethod(), copy.nsfFeeMethod());
            assertEquals(templateRow.nsfFeeAmount(), copy.nsfFeeAmount());
            assertEquals(templateRow.waterMethod(), copy.waterMethod());
            assertEquals(templateRow.trashMethod(), copy.trashMethod());
            return true;
        }));
        verify(auditService).recordInsert(eq("standard_terms"), any(), any());
    }

    // ── Patching ─────────────────────────────────────────────────────────────

    @Test
    void patchStandardTerms_shouldReturnEmpty_whenSetDoesNotExist() {
        // Arrange
        UUID unknownUUID = UUID.randomUUID();
        when(standardTermsRepository.findById(unknownUUID)).thenReturn(Optional.empty());

        // Act
        Optional<StandardTerms> result =
                standardTermsService.patchStandardTerms(unknownUUID, Map.of("name", "Nope"));

        // Assert
        assertTrue(result.isEmpty());
        verify(standardTermsRepository, never()).patch(any(), any());
        verifyNoInteractions(auditService);
    }

    @Test
    void patchStandardTerms_shouldNotRecordAudit_whenOnlyUpdatedAtChanged() {
        // Arrange -- patch always bumps updated_at, which is housekeeping, not a change
        StandardTermsRow before = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        StandardTermsRow after = withUpdatedAt(before, before.updatedAt().plusMinutes(1));
        when(standardTermsRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(standardTermsRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(after));

        // Act
        Map<String, Object> changes = new HashMap<>();
        changes.put("name", "WA Land Lease 2026"); // same value
        standardTermsService.patchStandardTerms(before.uuid(), changes);

        // Assert
        verifyNoInteractions(auditService);
    }

    @Test
    void patchStandardTerms_shouldRecordAudit_whenFieldActuallyChanges() {
        // Arrange
        StandardTermsRow before = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        StandardTermsRow after = withTargetRate(before, new BigDecimal("700.00"));
        when(standardTermsRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(standardTermsRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(after));

        // Act
        Map<String, Object> changes = new HashMap<>();
        changes.put("target_rate", new BigDecimal("700.00"));
        standardTermsService.patchStandardTerms(before.uuid(), changes);

        // Assert
        verify(auditService).recordUpdate(eq("standard_terms"), eq(before.uuid()), any(), any());
    }

    @Test
    void patchStandardTerms_shouldZeroTheAmount_whenMethodBecomesNone() {
        // Arrange
        StandardTermsRow before = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(standardTermsRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(before));

        // Act
        Map<String, Object> changes = new HashMap<>();
        changes.put("late_fee_method", "NONE");
        standardTermsService.patchStandardTerms(before.uuid(), changes);

        // Assert -- the CHECK constraint requires the amount to be zero
        verify(standardTermsRepository).patch(eq(before.uuid()), argThat(
                map -> BigDecimal.ZERO.equals(map.get("late_fee_amount"))));
    }

    @Test
    void patchStandardTerms_shouldNormaliseMethodCase() {
        // Arrange
        StandardTermsRow before = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(standardTermsRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(before));

        // Act
        Map<String, Object> changes = new HashMap<>();
        changes.put("water_method", "  submetered  ");
        standardTermsService.patchStandardTerms(before.uuid(), changes);

        // Assert
        verify(standardTermsRepository).patch(eq(before.uuid()), argThat(
                map -> "SUBMETERED".equals(map.get("water_method"))));
    }

    @Test
    void patchStandardTerms_shouldKeepTheExistingAmount_whenOnlyTheMethodIsPatched() {
        // Arrange -- before already has a FLAT late fee of 65
        StandardTermsRow before = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(standardTermsRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(before));

        // Act
        Map<String, Object> changes = new HashMap<>();
        changes.put("late_fee_method", "FLAT");
        standardTermsService.patchStandardTerms(before.uuid(), changes);

        // Assert -- patching one half of the pair must not wipe the other
        verify(standardTermsRepository).patch(eq(before.uuid()), argThat(
                map -> new BigDecimal("65.00").compareTo((BigDecimal) map.get("late_fee_amount")) == 0));
    }

    @Test
    void patchStandardTerms_shouldThrow_whenFlatMethodIsGivenZero() {
        // Arrange
        StandardTermsRow before = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(before.uuid())).thenReturn(Optional.of(before));

        // Act / Assert
        Map<String, Object> changes = new HashMap<>();
        changes.put("late_fee_method", "FLAT");
        changes.put("late_fee_amount", BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,
                () -> standardTermsService.patchStandardTerms(before.uuid(), changes));
        verify(standardTermsRepository, never()).patch(any(), any());
    }

    @Test
    void patchStandardTerms_shouldThrow_whenMethodIsNotInTheVocabulary() {
        // Arrange
        StandardTermsRow before = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(before.uuid())).thenReturn(Optional.of(before));

        // Act / Assert
        Map<String, Object> changes = new HashMap<>();
        changes.put("water_method", "BANANA");
        assertThrows(IllegalArgumentException.class,
                () -> standardTermsService.patchStandardTerms(before.uuid(), changes));
        verify(standardTermsRepository, never()).patch(any(), any());
    }

    @Test
    void patchStandardTerms_shouldThrow_whenTrashIsSetToSubmetered() {
        // Arrange -- trash has no submetered option
        StandardTermsRow before = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(before.uuid())).thenReturn(Optional.of(before));

        // Act / Assert
        Map<String, Object> changes = new HashMap<>();
        changes.put("trash_method", "SUBMETERED");
        assertThrows(IllegalArgumentException.class,
                () -> standardTermsService.patchStandardTerms(before.uuid(), changes));
    }

    // FAILS TODAY. FEE_METHODS in the service is Set.of("NONE","FLAT"), but FeeMethod
    // now carries PERCENT_OF_RENT and BANK_OR_FLAT -- the two methods the real WA lease
    // actually uses. This is the test that says so.
    @Test
    void patchStandardTerms_shouldAcceptPercentOfRent_andKeepItsAmount() {
        // Arrange
        StandardTermsRow before = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(standardTermsRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(before));

        // Act -- "one and a half percent of the monthly lot rent"
        Map<String, Object> changes = new HashMap<>();
        changes.put("late_fee_method", "PERCENT_OF_RENT");
        changes.put("late_fee_amount", new BigDecimal("1.5"));
        standardTermsService.patchStandardTerms(before.uuid(), changes);

        // Assert -- the percentage must survive, not be zeroed as a non-FLAT method
        verify(standardTermsRepository).patch(eq(before.uuid()), argThat(
                map -> new BigDecimal("1.5").compareTo((BigDecimal) map.get("late_fee_amount")) == 0));
    }

    // FAILS TODAY, same cause.
    @Test
    void patchStandardTerms_shouldAcceptBankOrFlat_andKeepItsFloor() {
        // Arrange
        StandardTermsRow before = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(standardTermsRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(before));

        // Act -- "$35.00, or as charged by the financial institution, whichever is greater"
        Map<String, Object> changes = new HashMap<>();
        changes.put("nsf_fee_method", "BANK_OR_FLAT");
        changes.put("nsf_fee_amount", new BigDecimal("35.00"));
        standardTermsService.patchStandardTerms(before.uuid(), changes);

        // Assert
        verify(standardTermsRepository).patch(eq(before.uuid()), argThat(
                map -> new BigDecimal("35.00").compareTo((BigDecimal) map.get("nsf_fee_amount")) == 0));
    }

    // Guards the vocabulary against drifting from the enum again.
    @Test
    void feeMethodVocabulary_shouldCoverEveryEnumConstant() {
        // Arrange
        StandardTermsRow before = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(standardTermsRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(before));

        // Act / Assert -- every FeeMethod must be a value the service will accept
        for (FeeMethod method : FeeMethod.values()) {
            Map<String, Object> changes = new HashMap<>();
            changes.put("late_fee_method", method.name());
            if (method != FeeMethod.NONE) {
                changes.put("late_fee_amount", new BigDecimal("10.00"));
            }
            assertDoesNotThrow(
                    () -> standardTermsService.patchStandardTerms(before.uuid(), changes),
                    "service rejected FeeMethod." + method);
        }
    }

    // ── Reads and delete ─────────────────────────────────────────────────────

    @Test
    void findForProperty_shouldReturnEmpty_whenThePropertyHasNoSetForThatType() {
        // Arrange
        UUID property = UUID.randomUUID();
        when(standardTermsRepository.findByPropertyAndAgreementType(property, AgreementType.STORAGE))
                .thenReturn(Optional.empty());

        // Act
        Optional<StandardTerms> result =
                standardTermsService.findForProperty(property, AgreementType.STORAGE);

        // Assert -- no set copied in means the property may not offer that agreement
        assertTrue(result.isEmpty());
    }

    @Test
    void findGlobalTemplates_shouldMapEveryRow() {
        // Arrange
        when(standardTermsRepository.findGlobalTemplates()).thenReturn(List.of(
                template(UUID.randomUUID(), "Standard Manufactured Home Lot Terms", AgreementType.LAND),
                template(UUID.randomUUID(), "Standard Residential Terms", AgreementType.RESIDENTIAL)));

        // Act
        List<StandardTerms> result = standardTermsService.findGlobalTemplates();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.property() == null));
    }

    @Test
    void deleteStandardTerms_shouldReturnTrue_andRecordAudit_whenSetExists() {
        // Arrange
        StandardTermsRow stubRow = template(UUID.randomUUID(), "WA Land Lease 2026", AgreementType.LAND);
        when(standardTermsRepository.findById(stubRow.uuid())).thenReturn(Optional.of(stubRow));
        when(standardTermsRepository.softDelete(stubRow.uuid())).thenReturn(true);

        // Act
        boolean result = standardTermsService.deleteStandardTerms(stubRow.uuid());

        // Assert
        assertTrue(result);
        verify(auditService).recordDelete(eq("standard_terms"), eq(stubRow.uuid()), any());
    }

    @Test
    void deleteStandardTerms_shouldReturnFalse_andNotRecordAudit_whenSetDoesNotExist() {
        // Arrange
        UUID unknownUUID = UUID.randomUUID();
        when(standardTermsRepository.findById(unknownUUID)).thenReturn(Optional.empty());

        // Act
        boolean result = standardTermsService.deleteStandardTerms(unknownUUID);

        // Assert
        assertFalse(result);
        verify(standardTermsRepository, never()).softDelete(any());
        verifyNoInteractions(auditService);
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    /** A global template carrying the V1 defaults, as the database would return it. */
    private static StandardTermsRow template(UUID uuid, String name, AgreementType agreementType) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new StandardTermsRow(
                uuid, null, null, name, agreementType, BigDecimal.ZERO,
                new BigDecimal("45.00"), 2, 4,
                new BigDecimal("45.00"), 2,
                1, 7,
                FeeMethod.FLAT, new BigDecimal("65.00"),
                FeeMethod.FLAT, new BigDecimal("35.00"),
                FeeMethod.FLAT, new BigDecimal("65.00"),
                UtilityMethod.NONE, BigDecimal.ZERO,
                UtilityMethod.NONE, BigDecimal.ZERO,
                UtilityMethod.NONE, BigDecimal.ZERO,
                UtilityMethod.NONE, BigDecimal.ZERO,
                null, now, now, SystemPrincipal.AGENT_UUID, null);
    }

    private static StandardTermsRow withUpdatedAt(StandardTermsRow row, OffsetDateTime updatedAt) {
        return new StandardTermsRow(
                row.uuid(), row.property(), row.copiedFrom(), row.name(), row.agreementType(), row.targetRate(),
                row.carFee(), row.allowedCars(), row.carsMax(), row.petFee(), row.allowedPets(),
                row.paymentDueDay(), row.gracePeriodDays(),
                row.ruleViolationFeeMethod(), row.ruleViolationFeeAmount(),
                row.nsfFeeMethod(), row.nsfFeeAmount(),
                row.lateFeeMethod(), row.lateFeeAmount(),
                row.waterMethod(), row.waterFlatAmount(),
                row.powerMethod(), row.powerFlatAmount(),
                row.sewerMethod(), row.sewerFlatAmount(),
                row.trashMethod(), row.trashFlatAmount(),
                row.note(), row.createdAt(), updatedAt, row.createdBy(), row.deletedAt());
    }

    private static StandardTermsRow withTargetRate(StandardTermsRow row, BigDecimal targetRate) {
        return new StandardTermsRow(
                row.uuid(), row.property(), row.copiedFrom(), row.name(), row.agreementType(), targetRate,
                row.carFee(), row.allowedCars(), row.carsMax(), row.petFee(), row.allowedPets(),
                row.paymentDueDay(), row.gracePeriodDays(),
                row.ruleViolationFeeMethod(), row.ruleViolationFeeAmount(),
                row.nsfFeeMethod(), row.nsfFeeAmount(),
                row.lateFeeMethod(), row.lateFeeAmount(),
                row.waterMethod(), row.waterFlatAmount(),
                row.powerMethod(), row.powerFlatAmount(),
                row.sewerMethod(), row.sewerFlatAmount(),
                row.trashMethod(), row.trashFlatAmount(),
                row.note(), row.createdAt(), row.updatedAt(), row.createdBy(), row.deletedAt());
    }
}