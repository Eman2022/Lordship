package io.github.lordship.documenttemplate.internal;

import io.github.lordship.IntegrationTest;
import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.InstrumentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class DocumentTemplateRepositoryTest extends IntegrationTest {

    // The SYSTEM agent seeded in V10, which is what V11 attributes its rows to.
    static final UUID SYSTEM_AGENT = UUID.fromString("00000000-0000-7000-8000-000000000002");

    @Autowired
    DocumentTemplateRepository documentTemplateRepository;

    private DocumentTemplateRow save(String name, AgreementType agreementType, InstrumentType instrumentType) {
        return documentTemplateRepository.save(name, agreementType, instrumentType, SYSTEM_AGENT);
    }

    // ---- save ----------------------------------------------------------------

    @Test
    void save_shouldPersistTheThreeRequiredColumns_andStartAtVersionOne() {
        // Act
        DocumentTemplateRow saved = save("Scratch Lease", AgreementType.LAND, InstrumentType.LEASE);

        // Assert
        assertNotNull(saved.uuid());
        assertEquals("Scratch Lease", saved.name());
        assertEquals(AgreementType.LAND, saved.agreementType());
        assertEquals(InstrumentType.LEASE, saved.instrumentType());
        assertEquals(1, saved.version());
        assertNull(saved.note());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());
    }

    // Both enums are Postgres enum types, which need an explicit cast going in
    // and a RowMapper coming back out.
    @Test
    void save_shouldRoundTripBothEnumColumns() {
        // Act
        DocumentTemplateRow saved = save("Storage Notice", AgreementType.STORAGE, InstrumentType.PAY_OR_VACATE);
        Optional<DocumentTemplateRow> found = documentTemplateRepository.findById(saved.uuid());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(AgreementType.STORAGE, found.get().agreementType());
        assertEquals(InstrumentType.PAY_OR_VACATE, found.get().instrumentType());
    }

    // ---- findAll -------------------------------------------------------------

    // The bug that only appeared when a filter was omitted: a bare parameter
    // beside IS NULL gives Postgres nothing to infer a type from.
    @Test
    void findAll_shouldWork_withNoFiltersAtAll() {
        // Arrange
        save("Unfiltered", AgreementType.LAND, InstrumentType.LEASE);

        // Act & Assert
        assertDoesNotThrow(() -> documentTemplateRepository.findAll(null, null));
        assertFalse(documentTemplateRepository.findAll(null, null).isEmpty());
    }

    @Test
    void findAll_shouldNarrowByEitherFilter_orBoth() {
        // Arrange
        save("Land Lease", AgreementType.LAND, InstrumentType.LEASE);
        save("Land Notice", AgreementType.LAND, InstrumentType.INCREASE_NOTICE);
        save("Storage Lease", AgreementType.STORAGE, InstrumentType.LEASE);

        // Act & Assert
        assertTrue(documentTemplateRepository.findAll(AgreementType.LAND, null).size() >= 2);
        assertTrue(documentTemplateRepository.findAll(null, InstrumentType.INCREASE_NOTICE).stream()
                .allMatch(row -> row.instrumentType() == InstrumentType.INCREASE_NOTICE));

        List<DocumentTemplateRow> both =
                documentTemplateRepository.findAll(AgreementType.STORAGE, InstrumentType.LEASE);
        assertTrue(both.stream().anyMatch(row -> "Storage Lease".equals(row.name())));
        assertTrue(both.stream().noneMatch(row -> "Land Lease".equals(row.name())));
    }

    @Test
    void findAll_shouldReturnEmpty_whenNothingMatches() {
        assertTrue(documentTemplateRepository
                .findAll(AgreementType.UTILITY_SERVICE, InstrumentType.WAIVER).isEmpty());
    }

    // ---- findById ------------------------------------------------------------

    @Test
    void findById_shouldReturnEmpty_afterSoftDelete() {
        // Arrange
        DocumentTemplateRow saved = save("Doomed", AgreementType.LAND, InstrumentType.LEASE);

        // Act
        documentTemplateRepository.softDelete(saved.uuid());

        // Assert
        assertTrue(documentTemplateRepository.findById(saved.uuid()).isEmpty());
    }

    @Test
    void findById_shouldReturnEmpty_whenUuidIsUnknown() {
        assertTrue(documentTemplateRepository.findById(UUID.randomUUID()).isEmpty());
    }

    // ---- bumpVersion ---------------------------------------------------------

    // The number an instrument freezes as template_version, so it has to move
    // on every clause change.
    @Test
    void bumpVersion_shouldIncrementByOne_eachTime() {
        // Arrange
        DocumentTemplateRow saved = save("Versioned", AgreementType.LAND, InstrumentType.LEASE);

        // Act
        Optional<DocumentTemplateRow> once = documentTemplateRepository.bumpVersion(saved.uuid());
        Optional<DocumentTemplateRow> twice = documentTemplateRepository.bumpVersion(saved.uuid());

        // Assert
        assertTrue(once.isPresent());
        assertEquals(2, once.get().version());
        assertTrue(twice.isPresent());
        assertEquals(3, twice.get().version());
    }

    @Test
    void bumpVersion_shouldReturnEmpty_forASoftDeletedTemplate() {
        // Arrange
        DocumentTemplateRow saved = save("Gone", AgreementType.LAND, InstrumentType.LEASE);
        documentTemplateRepository.softDelete(saved.uuid());

        // Act & Assert
        assertTrue(documentTemplateRepository.bumpVersion(saved.uuid()).isEmpty());
    }

    // ---- isAssignedToAnyProperty ---------------------------------------------

    @Test
    void isAssignedToAnyProperty_shouldBeFalse_forAnUnusedTemplate() {
        // Arrange
        DocumentTemplateRow saved = save("Unassigned", AgreementType.LAND, InstrumentType.LEASE);

        // Act & Assert
        assertFalse(documentTemplateRepository.isAssignedToAnyProperty(saved.uuid()));
    }

    // ---- patch ---------------------------------------------------------------

    @Test
    void patch_shouldUpdateNameAndNote() {
        // Arrange
        DocumentTemplateRow saved = save("Before", AgreementType.LAND, InstrumentType.LEASE);

        // Act
        Optional<DocumentTemplateRow> patched = documentTemplateRepository.patch(
                saved.uuid(), Map.of("name", "After", "note", "Renamed for 2027"));

        // Assert
        assertTrue(patched.isPresent());
        assertEquals("After", patched.get().name());
        assertEquals("Renamed for 2027", patched.get().note());
    }

    // The two enums are not patchable: a document's kind is what the assignment
    // unique index depends on, and the composite FK exists to keep them fixed.
    @Test
    void patch_shouldThrow_whenAgreementTypeIsRequested() {
        // Arrange
        DocumentTemplateRow saved = save("Fixed kind", AgreementType.LAND, InstrumentType.LEASE);

        // Act & Assert
        // @Repository exception translation wraps the IllegalArgumentException.
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
                documentTemplateRepository.patch(saved.uuid(), Map.of("agreement_type", "STORAGE")));
    }

    @Test
    void patch_shouldThrow_whenVersionIsRequested() {
        // Arrange: version moves only through bumpVersion
        DocumentTemplateRow saved = save("Fixed version", AgreementType.LAND, InstrumentType.LEASE);

        // Act & Assert
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
                documentTemplateRepository.patch(saved.uuid(), Map.of("version", 99)));
    }

    @Test
    void patch_shouldReturnUnchangedRow_withEmptyChanges() {
        // Arrange
        DocumentTemplateRow saved = save("Untouched", AgreementType.LAND, InstrumentType.LEASE);

        // Act
        Optional<DocumentTemplateRow> patched = documentTemplateRepository.patch(saved.uuid(), Map.of());

        // Assert
        assertTrue(patched.isPresent());
        assertEquals("Untouched", patched.get().name());
    }

    @Test
    void patch_shouldReturnEmpty_whenRowIsSoftDeleted() {
        // Arrange
        DocumentTemplateRow saved = save("Deleted", AgreementType.LAND, InstrumentType.LEASE);
        documentTemplateRepository.softDelete(saved.uuid());

        // Act & Assert
        assertTrue(documentTemplateRepository.patch(saved.uuid(), Map.of("name", "x")).isEmpty());
    }

    // ---- softDelete ----------------------------------------------------------

    @Test
    void softDelete_shouldReturnTrue_thenFalseOnASecondCall() {
        // Arrange
        DocumentTemplateRow saved = save("Twice", AgreementType.LAND, InstrumentType.LEASE);

        // Act & Assert
        assertTrue(documentTemplateRepository.softDelete(saved.uuid()));
        assertFalse(documentTemplateRepository.softDelete(saved.uuid()));
    }

    @Test
    void softDelete_shouldReturnFalse_whenUuidIsUnknown() {
        assertFalse(documentTemplateRepository.softDelete(UUID.randomUUID()));
    }
}