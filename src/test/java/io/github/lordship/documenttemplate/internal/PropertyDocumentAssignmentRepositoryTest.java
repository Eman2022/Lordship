package io.github.lordship.documenttemplate.internal;

import io.github.lordship.IntegrationTest;
import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.InstrumentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class PropertyDocumentAssignmentRepositoryTest extends IntegrationTest {

    // The SYSTEM agent seeded in V10, which is what V11 attributes its rows to.
    static final UUID SYSTEM_AGENT = UUID.fromString("00000000-0000-7000-8000-000000000002");

    @Autowired
    DocumentTemplateRepository documentTemplateRepository;

    @Autowired
    PropertyDocumentAssignmentRepository assignmentRepository;

    private DocumentTemplateRow template(String name, AgreementType agreementType, InstrumentType instrumentType) {
        return documentTemplateRepository.save(name, agreementType, instrumentType, SYSTEM_AGENT);
    }

    private DocumentTemplateRow leaseTemplate(String name) {
        return template(name, AgreementType.LAND, InstrumentType.LEASE);
    }

    private UUID property(String code) {
        return testData.insertProperty(code).uuid();
    }

    /** Assign the way the service does: the kind comes off the template, never the caller. */
    private PropertyDocumentAssignmentRow assign(UUID propertyId, DocumentTemplateRow document) {
        return assignmentRepository.save(
                propertyId, document.uuid(), document.agreementType(), document.instrumentType(), SYSTEM_AGENT);
    }

    // ---- save ----------------------------------------------------------------

    @Test
    void save_shouldCopyTheKindDownFromTheTemplate() {
        // Arrange
        UUID propertyId = property("D001");
        DocumentTemplateRow document = template("Storage notice", AgreementType.STORAGE, InstrumentType.PAY_OR_VACATE);

        // Act
        PropertyDocumentAssignmentRow saved = assign(propertyId, document);

        // Assert
        assertNotNull(saved.uuid());
        assertEquals(propertyId, saved.property());
        assertEquals(document.uuid(), saved.documentTemplate());
        assertEquals(AgreementType.STORAGE, saved.agreementType());
        assertEquals(InstrumentType.PAY_OR_VACATE, saved.instrumentType());
        assertNull(saved.note());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());
    }

    // The composite FK is the whole reason those two columns are duplicated
    // here: a caller cannot file a lease template under "increase notice" and
    // have generate pick it up for the wrong thing.
    @Test
    void save_shouldBeRefusedByCompositeFk_whenTheKindDisagreesWithTheTemplate() {
        // Arrange
        UUID propertyId = property("D002");
        DocumentTemplateRow document = leaseTemplate("Land lease");

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () ->
                assignmentRepository.save(propertyId, document.uuid(),
                        AgreementType.STORAGE, InstrumentType.LEASE, SYSTEM_AGENT));
    }

    @Test
    void save_shouldBeRefusedByCompositeFk_whenTheInstrumentTypeDisagrees() {
        // Arrange
        UUID propertyId = property("D003");
        DocumentTemplateRow document = leaseTemplate("Land lease");

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () ->
                assignmentRepository.save(propertyId, document.uuid(),
                        AgreementType.LAND, InstrumentType.INCREASE_NOTICE, SYSTEM_AGENT));
    }

    @Test
    void save_shouldFail_whenPropertyDoesNotExist() {
        // Arrange
        DocumentTemplateRow document = leaseTemplate("Homeless lease");

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> assign(UUID.randomUUID(), document));
    }

    // ---- the one-document-per-kind rule --------------------------------------

    // "Generate the lease" has to resolve to exactly one document without the
    // office worker choosing, which is what the partial unique index buys.
    @Test
    void save_shouldBeRefused_whenThePropertyAlreadyHasThatKind() {
        // Arrange
        UUID propertyId = property("D004");
        assign(propertyId, leaseTemplate("First lease"));
        DocumentTemplateRow replacement = leaseTemplate("Second lease");

        // Act & Assert
        assertThrows(DuplicateKeyException.class, () -> assign(propertyId, replacement));
    }

    @Test
    void save_shouldAllowTheSameKind_atADifferentProperty() {
        // Arrange
        DocumentTemplateRow document = leaseTemplate("Shared lease");
        assign(property("D005"), document);

        // Act
        PropertyDocumentAssignmentRow second = assign(property("D006"), document);

        // Assert: one document, many parks
        assertNotNull(second.uuid());
    }

    @Test
    void save_shouldAllowSeveralKinds_atOneProperty() {
        // Arrange
        UUID propertyId = property("D007");

        // Act
        assign(propertyId, leaseTemplate("The lease"));
        assign(propertyId, template("The notice", AgreementType.LAND, InstrumentType.INCREASE_NOTICE));

        // Assert
        assertEquals(2, assignmentRepository.findByProperty(propertyId).size());
    }

    // The index is partial on deleted_at, so unassigning genuinely frees the
    // slot -- swapping a park onto a new lease is unassign then assign.
    @Test
    void save_shouldBeAllowedAgain_afterTheOldAssignmentIsUnassigned() {
        // Arrange
        UUID propertyId = property("D008");
        PropertyDocumentAssignmentRow old = assign(propertyId, leaseTemplate("Old lease"));
        assignmentRepository.softDelete(old.uuid());

        // Act
        PropertyDocumentAssignmentRow replacement = assign(propertyId, leaseTemplate("New lease"));

        // Assert
        assertNotNull(replacement.uuid());
        assertEquals(1, assignmentRepository.findByProperty(propertyId).size());
    }

    // ---- findByPropertyAndKind -----------------------------------------------

    @Test
    void findByPropertyAndKind_shouldReturnTheOneDocument() {
        // Arrange
        UUID propertyId = property("D009");
        DocumentTemplateRow document = leaseTemplate("Resolvable lease");
        assign(propertyId, document);
        assign(propertyId, template("Resolvable notice", AgreementType.LAND, InstrumentType.INCREASE_NOTICE));

        // Act
        Optional<PropertyDocumentAssignmentRow> found = assignmentRepository
                .findByPropertyAndKind(propertyId, AgreementType.LAND, InstrumentType.LEASE);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(document.uuid(), found.get().documentTemplate());
    }

    // Empty is a real answer, not a failure: it means this park cannot generate
    // that document at all.
    @Test
    void findByPropertyAndKind_shouldReturnEmpty_whenThePropertyHasNoSuchDocument() {
        // Arrange
        UUID propertyId = property("D010");
        assign(propertyId, leaseTemplate("Only a lease"));

        // Act & Assert
        assertTrue(assignmentRepository
                .findByPropertyAndKind(propertyId, AgreementType.LAND, InstrumentType.INCREASE_NOTICE)
                .isEmpty());
    }

    @Test
    void findByPropertyAndKind_shouldReturnEmpty_afterUnassignment() {
        // Arrange
        UUID propertyId = property("D011");
        PropertyDocumentAssignmentRow saved = assign(propertyId, leaseTemplate("Retired lease"));
        assignmentRepository.softDelete(saved.uuid());

        // Act & Assert
        assertTrue(assignmentRepository
                .findByPropertyAndKind(propertyId, AgreementType.LAND, InstrumentType.LEASE)
                .isEmpty());
    }

    @Test
    void findByPropertyAndKind_shouldNotLeakAcrossProperties() {
        // Arrange
        UUID mine = property("D012");
        UUID theirs = property("D013");
        assign(theirs, leaseTemplate("Their lease"));

        // Act & Assert
        assertTrue(assignmentRepository
                .findByPropertyAndKind(mine, AgreementType.LAND, InstrumentType.LEASE)
                .isEmpty());
    }

    // ---- findByProperty ------------------------------------------------------

    @Test
    void findByProperty_shouldOrderByKind_soAListReadsPredictably() {
        // Arrange
        UUID propertyId = property("D014");
        assign(propertyId, template("Storage lease", AgreementType.STORAGE, InstrumentType.LEASE));
        assign(propertyId, leaseTemplate("Land lease"));

        // Act
        List<PropertyDocumentAssignmentRow> found = assignmentRepository.findByProperty(propertyId);

        // Assert: LAND before STORAGE, whatever order they were assigned in
        assertEquals(2, found.size());
        assertEquals(AgreementType.LAND, found.get(0).agreementType());
        assertEquals(AgreementType.STORAGE, found.get(1).agreementType());
    }

    @Test
    void findByProperty_shouldExcludeUnassignedRows() {
        // Arrange
        UUID propertyId = property("D015");
        PropertyDocumentAssignmentRow removed = assign(propertyId, leaseTemplate("Removed lease"));
        assignmentRepository.softDelete(removed.uuid());

        // Act & Assert
        assertTrue(assignmentRepository.findByProperty(propertyId).isEmpty());
    }

    @Test
    void findByProperty_shouldReturnEmpty_whenPropertyIsUnknown() {
        assertTrue(assignmentRepository.findByProperty(UUID.randomUUID()).isEmpty());
    }

    // ---- findByDocumentTemplate ----------------------------------------------

    // The list behind a refusal to retire a document.
    @Test
    void findByDocumentTemplate_shouldReturnEveryParkUsingIt() {
        // Arrange
        DocumentTemplateRow document = leaseTemplate("Widely used lease");
        assign(property("D016"), document);
        assign(property("D017"), document);

        // Act & Assert
        assertEquals(2, assignmentRepository.findByDocumentTemplate(document.uuid()).size());
    }

    @Test
    void findByDocumentTemplate_shouldExcludeUnassignedRows() {
        // Arrange
        DocumentTemplateRow document = leaseTemplate("Half-retired lease");
        PropertyDocumentAssignmentRow removed = assign(property("D018"), document);
        assign(property("D019"), document);
        assignmentRepository.softDelete(removed.uuid());

        // Act & Assert
        assertEquals(1, assignmentRepository.findByDocumentTemplate(document.uuid()).size());
    }

    @Test
    void findByDocumentTemplate_shouldReturnEmpty_forAnUnassignedTemplate() {
        // Arrange
        DocumentTemplateRow document = leaseTemplate("Never assigned");

        // Act & Assert
        assertTrue(assignmentRepository.findByDocumentTemplate(document.uuid()).isEmpty());
    }

    // The other side of the same question, asked of the template repository.
    @Test
    void isAssignedToAnyProperty_shouldFlipTrue_onceAssigned() {
        // Arrange
        DocumentTemplateRow document = leaseTemplate("Becomes used");
        assertFalse(documentTemplateRepository.isAssignedToAnyProperty(document.uuid()));

        // Act
        assign(property("D020"), document);

        // Assert
        assertTrue(documentTemplateRepository.isAssignedToAnyProperty(document.uuid()));
    }

    // ---- patch ---------------------------------------------------------------

    @Test
    void patch_shouldUpdateTheNote() {
        // Arrange
        PropertyDocumentAssignmentRow saved = assign(property("D021"), leaseTemplate("Annotated lease"));

        // Act
        Optional<PropertyDocumentAssignmentRow> patched = assignmentRepository.patch(
                saved.uuid(), Map.of("note", "Adopted at the 2027 rent roll"));

        // Assert
        assertTrue(patched.isPresent());
        assertEquals("Adopted at the 2027 rent roll", patched.get().note());
    }

    // Repointing a park at a different document is unassign then assign, not a
    // patch -- otherwise the kind columns could drift from the new template's.
    @Test
    void patch_shouldThrow_whenTheDocumentTemplateIsRequested() {
        // Arrange
        PropertyDocumentAssignmentRow saved = assign(property("D022"), leaseTemplate("Fixed lease"));

        // Act & Assert
        // @Repository exception translation wraps the IllegalArgumentException.
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
                assignmentRepository.patch(saved.uuid(), Map.of("document_template", UUID.randomUUID())));
    }

    @Test
    void patch_shouldThrow_whenAgreementTypeIsRequested() {
        // Arrange
        PropertyDocumentAssignmentRow saved = assign(property("D023"), leaseTemplate("Fixed kind"));

        // Act & Assert
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
                assignmentRepository.patch(saved.uuid(), Map.of("agreement_type", "STORAGE")));
    }

    @Test
    void patch_shouldReturnUnchangedRow_withEmptyChanges() {
        // Arrange
        PropertyDocumentAssignmentRow saved = assign(property("D024"), leaseTemplate("Untouched lease"));

        // Act
        Optional<PropertyDocumentAssignmentRow> patched = assignmentRepository.patch(saved.uuid(), Map.of());

        // Assert
        assertTrue(patched.isPresent());
        assertEquals(saved.uuid(), patched.get().uuid());
    }

    @Test
    void patch_shouldReturnEmpty_whenRowIsSoftDeleted() {
        // Arrange
        PropertyDocumentAssignmentRow saved = assign(property("D025"), leaseTemplate("Deleted lease"));
        assignmentRepository.softDelete(saved.uuid());

        // Act & Assert
        assertTrue(assignmentRepository.patch(saved.uuid(), Map.of("note", "x")).isEmpty());
    }

    // ---- softDelete ----------------------------------------------------------

    @Test
    void softDelete_shouldReturnTrue_thenFalseOnASecondCall() {
        // Arrange
        PropertyDocumentAssignmentRow saved = assign(property("D026"), leaseTemplate("Twice lease"));

        // Act & Assert
        assertTrue(assignmentRepository.softDelete(saved.uuid()));
        assertFalse(assignmentRepository.softDelete(saved.uuid()));
    }

    @Test
    void softDelete_shouldReturnFalse_whenUuidIsUnknown() {
        assertFalse(assignmentRepository.softDelete(UUID.randomUUID()));
    }
}