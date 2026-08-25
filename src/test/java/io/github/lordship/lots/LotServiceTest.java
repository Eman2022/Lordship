package io.github.lordship.lots;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.lots.internal.*;
import io.github.lordship.shared.AgreementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LotServiceTest {

    @Mock
    LotRepository lotRepository;

    @Mock
    LotPermissibleAgreementTypeRepository lotPermissibleAgreementTypeRepository;

    @Mock
    AuditService auditService;

    @InjectMocks
    LotService lotService;

    // Grabs the before/after maps handed to AuditService.recordUpdate so tests can
    // assert the log holds only the fields that actually changed.
    @SuppressWarnings("unchecked")
    private AuditMapper.Diff capturedUpdate(UUID uuid) {
        ArgumentCaptor<Map<String, Object>> before = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> after = ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordUpdate(eq("lot"), eq(uuid), before.capture(), after.capture());
        return new AuditMapper.Diff(before.getValue(), after.getValue());
    }

    private LotRow stubRow(UUID propertyId) {
        return stubRow(UUID.randomUUID(), propertyId);
    }

    private LotRow stubRow(UUID uuid, UUID propertyId) {
        return new LotRow(
                uuid, propertyId, true, null,
                "12", "123 Main St", null, "Rental lot", "Front row", 1,
                null, OffsetDateTime.now(ZoneOffset.UTC), null
        );
    }

    private LotPermissibleAgreementTypeRow stubAgreementType(UUID lotId, AgreementType type, String rent) {
        return new LotPermissibleAgreementTypeRow(lotId, type, new BigDecimal(rent));
    }

    @Test
    void createLot_shouldInsertMinimalRow_andRecordAudit() {
        // Arrange
        UUID propertyId = UUID.randomUUID();
        LotRow savedRow = stubRow(propertyId);
        when(lotRepository.save(propertyId, "12")).thenReturn(savedRow);

        // Act
        Lot result = lotService.createLot(propertyId, "12");

        // Assert: only the two minimum columns reach the insert
        verify(lotRepository).save(propertyId, "12");

        assertEquals(savedRow.uuid(), result.uuid());
        assertEquals("12", result.lotNumber());

        // A brand-new lot has no permissible agreement types yet, and creating one
        // shouldn't need to ask that repository anything.
        assertEquals(List.of(), result.permissibleAgreementTypes());
        verifyNoInteractions(lotPermissibleAgreementTypeRepository);

        // Keys must be the record component names AuditMapper produces, matching every
        // other module's audit entries -- not the snake_case column names.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> auditCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordInsert(eq("lot"), eq(savedRow.uuid()), auditCaptor.capture());

        Map<String, Object> logged = auditCaptor.getValue();
        assertEquals(propertyId, logged.get("propertyId"));
        assertEquals("12", logged.get("lotNumber"));
        assertFalse(logged.containsKey("lot_number"));
        assertFalse(logged.containsKey("property_id"));
    }

    @Test
    void deleteLot_shouldReturnTrue_andRecordAudit_whenExists() {
        // Arrange
        LotRow existing = stubRow(UUID.randomUUID());
        when(lotRepository.findById(existing.uuid())).thenReturn(Optional.of(existing));
        when(lotRepository.softDelete(existing.uuid())).thenReturn(true);

        // Act
        boolean result = lotService.deleteLot(existing.uuid());

        // Assert
        assertTrue(result);
        verify(lotRepository).softDelete(existing.uuid());
        verifyNoInteractions(lotPermissibleAgreementTypeRepository);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordDelete(eq("lot"), eq(existing.uuid()), captor.capture());
        verify(auditService, never()).recordUpdate(any(), any(), any(), any());

        Map<String, Object> logged = captor.getValue();
        assertEquals(existing.propertyId(), logged.get("propertyId"));
        assertEquals(existing.lotNumber(), logged.get("lotNumber"));
        assertFalse(logged.containsKey("deleted_at"));
    }

    @Test
    void deleteLot_shouldReturnFalse_andNotRecordAudit_whenNotExists() {
        // Arrange
        UUID unknownUuid = UUID.randomUUID();
        when(lotRepository.findById(unknownUuid)).thenReturn(Optional.empty());

        // Act
        boolean result = lotService.deleteLot(unknownUuid);

        // Assert
        assertFalse(result);
        verify(lotRepository, never()).softDelete(any());
        verifyNoInteractions(auditService);
        verifyNoInteractions(lotPermissibleAgreementTypeRepository);
    }

    @Test
    void patchLot_shouldReturnUpdatedLot_andRecordAudit_whenFieldChanges() {
        // Arrange
        UUID propertyId = UUID.randomUUID();
        LotRow before = stubRow(propertyId);
        LotRow after = new LotRow(
                before.uuid(), propertyId, true, null,
                "14", before.lotAddress(), "3005051", before.description(), before.notes(), before.sortOrder(),
                before.shapeData(), before.createdAt(), null
        );
        when(lotRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(lotRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(after));

        List<LotPermissibleAgreementTypeRow> agreementTypes =
                List.of(stubAgreementType(before.uuid(), AgreementType.RESIDENTIAL, "350.00"));
        when(lotPermissibleAgreementTypeRepository.findByLotId(before.uuid())).thenReturn(agreementTypes);

        Map<String, Object> changes = new HashMap<>();
        changes.put("lot_number", "14");
        changes.put("lot_parcel", "3005051");

        // Act
        Optional<Lot> result = lotService.patchLot(before.uuid(), changes);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("14", result.get().lotNumber());
        // Patched lots pick up their current permissible agreement types, since
        // patching the base row doesn't touch that table.
        assertEquals(1, result.get().permissibleAgreementTypes().size());
        assertEquals(AgreementType.RESIDENTIAL, result.get().permissibleAgreementTypes().get(0).agreementType());

        // Only lotNumber and lot Parcel moved, so the log must not carry the untouched fields.
        AuditMapper.Diff logged = capturedUpdate(before.uuid());
        assertEquals(Set.of("lotNumber", "lotParcel"), logged.before().keySet());
        assertEquals("12", logged.before().get("lotNumber"));
        assertEquals(Set.of("lotNumber", "lotParcel"), logged.after().keySet());
        assertEquals("14", logged.after().get("lotNumber"));
        assertEquals("3005051", logged.after().get("lotParcel"));
    }

    @Test
    void patchLot_shouldNotRecordAudit_whenNoDiffProduced() {
        // Arrange
        LotRow before = stubRow(UUID.randomUUID());
        // A separate instance carrying the same values: the diff has to come out empty on
        // value equality, not because the stub handed back the very same object.
        LotRow after = new LotRow(
                before.uuid(), before.propertyId(), before.isRentable(), before.notRentableReason(),
                before.lotNumber(), before.lotAddress(), before.lotParcel(), before.description(), before.notes(),
                before.sortOrder(), before.shapeData(), before.createdAt(), before.deletedAt()
        );
        when(lotRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(lotRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(after));
        when(lotPermissibleAgreementTypeRepository.findByLotId(before.uuid())).thenReturn(List.of());

        Map<String, Object> changes = new HashMap<>();
        changes.put("lot_number", before.lotNumber());

        // Act
        lotService.patchLot(before.uuid(), changes);

        // Assert
        verifyNoInteractions(auditService);
    }


    @Test
    void patchLot_shouldReturnEmpty_andNotRecordAudit_whenNotExists() {
        // Arrange
        UUID unknownUuid = UUID.randomUUID();
        when(lotRepository.findById(unknownUuid)).thenReturn(Optional.empty());

        Map<String, Object> changes = new HashMap<>();
        changes.put("lot_number", "14");

        // Act
        Optional<Lot> result = lotService.patchLot(unknownUuid, changes);

        // Assert
        assertTrue(result.isEmpty());
        verify(lotRepository, never()).patch(any(), any());
        verifyNoInteractions(auditService);
        verifyNoInteractions(lotPermissibleAgreementTypeRepository);
    }

    @Test
    void findById_shouldReturnLot_withMergedAgreementTypes_whenFound() {
        // Arrange
        LotRow existing = stubRow(UUID.randomUUID());
        when(lotRepository.findById(existing.uuid())).thenReturn(Optional.of(existing));
        List<LotPermissibleAgreementTypeRow> agreementTypes =
                List.of(stubAgreementType(existing.uuid(), AgreementType.STORAGE, "80.00"));
        when(lotPermissibleAgreementTypeRepository.findByLotId(existing.uuid())).thenReturn(agreementTypes);

        // Act
        Optional<Lot> result = lotService.findById(existing.uuid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(existing.uuid(), result.get().uuid());
        assertEquals(1, result.get().permissibleAgreementTypes().size());
        assertEquals(AgreementType.STORAGE, result.get().permissibleAgreementTypes().get(0).agreementType());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        // Arrange
        UUID unknownUuid = UUID.randomUUID();
        when(lotRepository.findById(unknownUuid)).thenReturn(Optional.empty());

        // Act
        Optional<Lot> result = lotService.findById(unknownUuid);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(lotPermissibleAgreementTypeRepository);
    }

    @Test
    void findByProperty_shouldBatchAgreementTypeLookup_andMergePerLot() {
        // Arrange: two lots, only one of which has a permissible agreement type, to
        // confirm the grouping doesn't just zip results back in list order.
        UUID propertyId = UUID.randomUUID();
        LotRow lotA = stubRow(UUID.randomUUID(), propertyId);
        LotRow lotB = stubRow(UUID.randomUUID(), propertyId);
        when(lotRepository.findByProperty("PARK1")).thenReturn(List.of(lotA, lotB));
        when(lotPermissibleAgreementTypeRepository.findByLotIds(List.of(lotA.uuid(), lotB.uuid())))
                .thenReturn(List.of(stubAgreementType(lotB.uuid(), AgreementType.COMMERCIAL, "500.00")));

        // Act
        List<Lot> result = lotService.findByProperty("PARK1");

        // Assert
        assertEquals(2, result.size());
        Lot resultA = result.stream().filter(l -> l.uuid().equals(lotA.uuid())).findFirst().orElseThrow();
        Lot resultB = result.stream().filter(l -> l.uuid().equals(lotB.uuid())).findFirst().orElseThrow();
        assertEquals(List.of(), resultA.permissibleAgreementTypes());
        assertEquals(1, resultB.permissibleAgreementTypes().size());
        assertEquals(AgreementType.COMMERCIAL, resultB.permissibleAgreementTypes().get(0).agreementType());

        // One batched call for both lots, not one call per lot.
        verify(lotPermissibleAgreementTypeRepository, times(1)).findByLotIds(any());
    }

    @Test
    void findByProperty_shouldReturnEmptyList_withoutQueryingAgreementTypes_whenNoLotsFound() {
        // Arrange
        when(lotRepository.findByProperty("EMPTY")).thenReturn(List.of());

        // Act
        List<Lot> result = lotService.findByProperty("EMPTY");

        // Assert
        assertEquals(List.of(), result);
        verifyNoInteractions(lotPermissibleAgreementTypeRepository);
    }
}