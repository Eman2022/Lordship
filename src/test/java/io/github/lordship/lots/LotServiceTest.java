package io.github.lordship.lots;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.lots.internal.LotRepository;
import io.github.lordship.lots.internal.LotRow;
import io.github.lordship.lots.internal.LotUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
        return new LotRow(
                UUID.randomUUID(), propertyId, "12",
                "Rental lot", "Front row", 1,
                OffsetDateTime.now(ZoneOffset.UTC), null
        );
    }

    @Test
    void createLot_shouldReturnLot_andRecordAudit() {
        // Arrange
        UUID propertyId = UUID.randomUUID();
        LotRow savedRow = stubRow(propertyId);
        when(lotRepository.save(any())).thenReturn(savedRow);

        LotCreationRequest request = new LotCreationRequest(
                propertyId, "12", "Rental lot", "Front row", 1
        );

        // Act
        Lot result = lotService.createLot(request);

        // Assert
        assertEquals(savedRow.uuid(), result.uuid());
        assertEquals("12", result.lotNumber());
        assertEquals("Rental lot", result.description());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordInsert(eq("lot"), eq(savedRow.uuid()), captor.capture());

        // Keys must be the record component names AuditMapper produces, matching every
        // other module's audit entries -- not the snake_case column names.
        Map<String, Object> logged = captor.getValue();
        assertEquals(propertyId, logged.get("propertyId"));
        assertEquals("12", logged.get("lotNumber"));
        assertEquals("Rental lot", logged.get("description"));
        assertEquals("Front row", logged.get("notes"));
        assertEquals(1, logged.get("sortOrder"));
        assertFalse(logged.containsKey("lot_number"));
        assertFalse(logged.containsKey("property_id"));
        assertFalse(logged.containsKey("sort_order"));
    }

    @Test
    void updateLot_shouldReturnUpdatedLot_andRecordAudit_whenExists() {
        // Arrange
        UUID propertyId = UUID.randomUUID();
        LotRow existing = stubRow(propertyId);
        LotRow updated = new LotRow(
                existing.uuid(), propertyId, "14",
                "Vacant lot", "Ready for assignment", 3,
                existing.createdAt(), null
        );
        when(lotRepository.findById(existing.uuid())).thenReturn(Optional.of(existing));
        when(lotRepository.update(any())).thenReturn(updated);

        LotUpdateRequest request = new LotUpdateRequest("14", "Vacant lot", "Ready for assignment", 3);

        // Act
        Optional<Lot> result = lotService.updateLot(existing.uuid(), request);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("14", result.get().lotNumber());
        assertEquals("Vacant lot", result.get().description());

        // propertyId is unchanged (and not editable here), so it must stay out of the log.
        AuditMapper.Diff logged = capturedUpdate(existing.uuid());
        assertEquals(Set.of("lotNumber", "description", "notes", "sortOrder"), logged.before().keySet());
        assertEquals(logged.before().keySet(), logged.after().keySet());
        assertEquals("12", logged.before().get("lotNumber"));
        assertEquals("14", logged.after().get("lotNumber"));
    }

    @Test
    void updateLot_shouldReturnEmpty_andNotRecordAudit_whenNotExists() {
        // Arrange
        UUID unknownUuid = UUID.randomUUID();
        when(lotRepository.findById(unknownUuid)).thenReturn(Optional.empty());

        LotUpdateRequest request = new LotUpdateRequest("14", "Vacant lot", "Ready for assignment", 3);

        // Act
        Optional<Lot> result = lotService.updateLot(unknownUuid, request);

        // Assert
        assertTrue(result.isEmpty());
        verify(lotRepository, never()).update(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteLot_shouldReturnTrue_andRecordAudit_whenExists() {
        // Arrange
        LotRow existing = stubRow(UUID.randomUUID());
        when(lotRepository.findById(existing.uuid())).thenReturn(Optional.of(existing));

        // Act
        boolean result = lotService.deleteLot(existing.uuid());

        // Assert
        assertTrue(result);
        verify(lotRepository).softDelete(existing.uuid());
        verify(auditService).recordUpdate(eq("lot"), eq(existing.uuid()), any(), any());
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
    }

    @Test
    void patchLot_shouldReturnUpdatedLot_andRecordAudit_whenFieldChanges() {
        // Arrange
        UUID propertyId = UUID.randomUUID();
        LotRow before = stubRow(propertyId);
        LotRow after = new LotRow(
                before.uuid(), propertyId, "14",
                "Rental lot", "Front row", 1,
                before.createdAt(), null
        );
        when(lotRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(lotRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(after));

        Map<String, Object> changes = new HashMap<>();
        changes.put("lot_number", "14");

        // Act
        Optional<Lot> result = lotService.patchLot(before.uuid(), changes);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("14", result.get().lotNumber());

        // Only lotNumber moved, so the log must not carry the four untouched fields.
        AuditMapper.Diff logged = capturedUpdate(before.uuid());
        assertEquals(Set.of("lotNumber"), logged.before().keySet());
        assertEquals("12", logged.before().get("lotNumber"));
        assertEquals(Set.of("lotNumber"), logged.after().keySet());
        assertEquals("14", logged.after().get("lotNumber"));
    }

    @Test
    void patchLot_shouldNotRecordAudit_whenNoDiffProduced() {
        // Arrange
        LotRow before = stubRow(UUID.randomUUID());
        // A separate instance carrying the same values: the diff has to come out empty on
        // value equality, not because the stub handed back the very same object.
        LotRow after = new LotRow(
                before.uuid(), before.propertyId(), before.lotNumber(),
                before.description(), before.notes(), before.sortOrder(),
                before.createdAt(), before.deletedAt()
        );
        when(lotRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(lotRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(after));

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
    }

    @Test
    void findById_shouldReturnLot_whenFound() {
        // Arrange
        LotRow existing = stubRow(UUID.randomUUID());
        when(lotRepository.findById(existing.uuid())).thenReturn(Optional.of(existing));

        // Act
        Optional<Lot> result = lotService.findById(existing.uuid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(existing.uuid(), result.get().uuid());
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
    }
}
