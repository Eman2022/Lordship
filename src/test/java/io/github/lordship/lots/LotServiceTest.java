package io.github.lordship.lots;

import io.github.lordship.audit.AuditService;
import io.github.lordship.lots.internal.LotRepository;
import io.github.lordship.lots.internal.LotRow;
import io.github.lordship.lots.internal.LotUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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

    private LotRow stubRow(UUID propertyId) {
        return new LotRow(
                UUID.randomUUID(), propertyId, "12",
                "Rental lot", "Front row", 1,
                LocalDateTime.now(), null
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
        verify(auditService).recordInsert(eq("lot"), eq(savedRow.uuid()), any());
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
        verify(auditService).recordUpdate(eq("lot"), eq(existing.uuid()), any(), any());
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
