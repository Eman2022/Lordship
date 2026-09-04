package io.github.lordship.documenttemplate;

import io.github.lordship.audit.ActingAgent;
import io.github.lordship.audit.AuditContext;
import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.documenttemplate.internal.PropertyDocumentAssignmentRepository;
import io.github.lordship.documenttemplate.internal.PropertyDocumentAssignmentRow;
import io.github.lordship.properties.PropertyService;
import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.InstrumentType;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Which documents a park may generate. Assigning is the act that authorises a
 * property to produce a kind of paper at all -- a park with no LEASE assignment
 * cannot generate a lease, however complete the global template is.
 *
 * <p>Deliberately a reference rather than a copy, so an edit to the global
 * wording reaches every park. That is the opposite of {@code terms_template},
 * which is copied in, and the asymmetry is the point: a statutory correction
 * should land everywhere, a price change should not rewrite what existing
 * tenants already agreed.
 */
@Service
public class PropertyDocumentAssignmentService {

    private final PropertyDocumentAssignmentRepository assignmentRepository;
    private final DocumentTemplateService documentTemplateService;
    private final PropertyService propertyService;
    private final AuditService auditService;
    private final AuditContext auditContext;

    public PropertyDocumentAssignmentService(PropertyDocumentAssignmentRepository assignmentRepository,
                                             DocumentTemplateService documentTemplateService,
                                             PropertyService propertyService,
                                             AuditService auditService,
                                             AuditContext auditContext) {
        this.assignmentRepository = assignmentRepository;
        this.documentTemplateService = documentTemplateService;
        this.propertyService = propertyService;
        this.auditService = auditService;
        this.auditContext = auditContext;
    }

    public List<PropertyDocumentAssignment> findByProperty(UUID propertyId) {
        return assignmentRepository.findByProperty(propertyId).stream()
                .map(this::hydrate)
                .toList();
    }

    public Optional<PropertyDocumentAssignment> findById(UUID uuid) {
        return assignmentRepository.findById(uuid).map(this::hydrate);
    }

    /**
     * The document this park uses for one kind of deal. This is what generate
     * calls, and why the office worker is never asked to pick a document: the
     * unique index guarantees at most one answer.
     */
    public Optional<PropertyDocumentAssignment> findForProperty(UUID propertyId,
                                                                AgreementType agreementType,
                                                                InstrumentType instrumentType) {
        return assignmentRepository
                .findByPropertyAndKind(propertyId, agreementType, instrumentType)
                .map(this::hydrate);
    }

    /**
     * Empty means the property or the document does not exist. A park that
     * already has a document for this kind of deal is a rule violation rather
     * than a missing record, so that comes back as a conflict naming the
     * document already in place.
     */
    @Transactional
    public Optional<PropertyDocumentAssignment> assign(UUID propertyId, UUID documentTemplateId) {
        if (propertyService.findByPropertyId(propertyId).isEmpty()) {
            return Optional.empty();
        }

        Optional<DocumentTemplate> templateOpt = documentTemplateService.findById(documentTemplateId);
        if (templateOpt.isEmpty()) {
            return Optional.empty();
        }
        DocumentTemplate template = templateOpt.get();

        assignmentRepository
                .findByPropertyAndKind(propertyId, template.agreementType(), template.instrumentType())
                .ifPresent(existing -> {
                    String inUse = documentTemplateService.findById(existing.documentTemplate())
                            .map(DocumentTemplate::name)
                            .orElse(existing.documentTemplate().toString());

                    throw new IllegalStateException(
                            "This property already uses \"" + inUse + "\" for "
                                    + template.agreementType() + " " + template.instrumentType()
                                    + ". Unassign that one first -- \"generate the "
                                    + template.instrumentType().name().toLowerCase()
                                    + "\" has to resolve to exactly one document");
                });

        PropertyDocumentAssignmentRow saved = assignmentRepository.save(
                propertyId,
                documentTemplateId,
                template.agreementType(),
                template.instrumentType(),
                ActingAgent.resolve(auditContext));

        auditService.recordInsert(
                "property_document_assignment", saved.uuid(), AuditMapper.toMap(saved));

        return Optional.of(saved.toPropertyDocumentAssignment(summaryOf(template)));
    }

    @Transactional
    public Optional<PropertyDocumentAssignment> patchAssignment(UUID uuid, Map<String, Object> changes) {
        Optional<PropertyDocumentAssignmentRow> beforeOpt = assignmentRepository.findById(uuid);
        if (beforeOpt.isEmpty()) {
            return Optional.empty();
        }
        PropertyDocumentAssignmentRow before = beforeOpt.get();

        Optional<PropertyDocumentAssignmentRow> afterOpt = assignmentRepository.patch(uuid, changes);
        if (afterOpt.isEmpty()) {
            return Optional.empty();
        }

        AuditMapper.Diff diff = AuditMapper.diff(before, afterOpt.get());
        if (!diff.before().isEmpty()) {
            auditService.recordUpdate("property_document_assignment", uuid, diff.before(), diff.after());
        }

        return Optional.of(hydrate(afterOpt.get()));
    }

    /**
     * Stops this park generating that kind of document from now on. Nothing
     * already generated is affected -- an instrument holds its own wording, and
     * a lease that went out last year does not become unsigned because someone
     * changed a setup screen.
     */
    @Transactional
    public boolean unassign(UUID uuid) {
        return assignmentRepository.findById(uuid).map(existing -> {
            if (!assignmentRepository.softDelete(uuid)) {
                return false;
            }
            auditService.recordDelete(
                    "property_document_assignment", uuid, AuditMapper.toMap(existing));
            return true;
        }).orElse(false);
    }

    // ---- internals -----------------------------------------------------------

    // One place an assignment gets built. The template comes back as a summary:
    // a park's document list wants names and versions, not sixty clause bodies
    // per row.
    private PropertyDocumentAssignment hydrate(PropertyDocumentAssignmentRow row) {
        DocumentTemplate document = documentTemplateService.findById(row.documentTemplate())
                .map(PropertyDocumentAssignmentService::summaryOf)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Assignment " + row.uuid() + " points at a document that no longer exists"));

        return row.toPropertyDocumentAssignment(document);
    }

    private static DocumentTemplate summaryOf(DocumentTemplate template) {
        return new DocumentTemplate(
                template.uuid(),
                template.name(),
                template.agreementType(),
                template.instrumentType(),
                template.version(),
                template.note(),
                template.createdAt(),
                template.deletedAt(),
                List.of());
    }
}