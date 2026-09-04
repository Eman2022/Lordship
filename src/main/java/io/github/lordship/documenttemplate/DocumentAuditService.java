package io.github.lordship.documenttemplate;

import io.github.lordship.tenancyterms.ChargeTermConfiguration;
import io.github.lordship.tenancyterms.TenancyChargeTermService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Measures a park's assigned documents against the deals actually in force
 * there.
 *
 * <p>The two template-only checks catch drift inside the model: a clause that
 * prints a figure it has not guarded, a method whose branches are half written.
 * Neither knows a tenant exists. This one starts from the charge terms and asks
 * the question that matters -- if we generated every lease at this park today,
 * would any of them come out missing a paragraph?
 *
 * <p>It is cheap because configurations collapse. Six thousand lots resolve to
 * a handful of distinct method combinations, since the figures differ from
 * tenant to tenant but never decide whether a paragraph prints.
 */
@Service
public class DocumentAuditService {

    private final PropertyDocumentAssignmentService assignmentService;
    private final DocumentTemplateService documentTemplateService;
    private final TenancyChargeTermService tenancyChargeTermService;

    public DocumentAuditService(PropertyDocumentAssignmentService assignmentService,
                                DocumentTemplateService documentTemplateService,
                                TenancyChargeTermService tenancyChargeTermService) {
        this.assignmentService = assignmentService;
        this.documentTemplateService = documentTemplateService;
        this.tenancyChargeTermService = tenancyChargeTermService;
    }

    public DocumentAudit auditProperty(UUID propertyId) {
        List<ChargeTermConfiguration> configurations =
                tenancyChargeTermService.findConfigurationsInForceByProperty(propertyId);

        List<DocumentAudit.DocumentFinding> findings = new ArrayList<>();

        for (PropertyDocumentAssignment assignment : assignmentService.findByProperty(propertyId)) {
            // The assignment carries a summary of the template, which has no
            // sections; the audit needs the clauses, so read the full document
            // once per assignment rather than once per configuration.
            Optional<DocumentTemplate> templateOpt =
                    documentTemplateService.findById(assignment.document().uuid());

            if (templateOpt.isEmpty()) {
                continue;
            }
            DocumentTemplate template = templateOpt.get();

            // A document for LAND leases has nothing to say about a storage deal.
            List<ChargeTermConfiguration> relevant = configurations.stream()
                    .filter(configuration -> assignment.agreementType()
                            .name().equals(configuration.agreementType()))
                    .toList();

            List<DocumentAudit.Gap> gaps = new ArrayList<>();
            for (ChargeTermConfiguration configuration : relevant) {
                List<String> missing = missingFor(template, configuration.methodValues());
                if (!missing.isEmpty()) {
                    gaps.add(new DocumentAudit.Gap(
                            configuration.methodValues(), configuration.tenancyCount(), missing));
                }
            }

            findings.add(new DocumentAudit.DocumentFinding(
                    assignment.uuid(),
                    template.uuid(),
                    template.name(),
                    template.version(),
                    assignment.agreementType(),
                    assignment.instrumentType(),
                    relevant.size(),
                    List.copyOf(gaps)));
        }

        return new DocumentAudit(propertyId, List.copyOf(findings));
    }

    /**
     * For one configuration: every method this document branches on where the
     * configuration's value has no clause at all.
     *
     * <p>Only methods the document already branches on are checked. A document
     * that never mentions trash is not missing a trash clause -- it simply does
     * not discuss trash, which may be entirely correct. What this catches is a
     * document that clearly does care about a method, and handles every value
     * except the one this park is actually on.
     */
    private static List<String> missingFor(DocumentTemplate template, Map<String, String> methodValues) {
        Set<String> branchedOn = template.clausesInOrder().stream()
                .map(TemplateClause::conditionField)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> missing = new ArrayList<>();
        for (String field : branchedOn) {
            String actual = methodValues.get(field);
            if (actual == null) {
                continue;
            }

            boolean covered = template.clausesInOrder().stream()
                    .filter(clause -> field.equals(clause.conditionField()))
                    .anyMatch(clause -> clause.conditionValues().contains(actual));

            if (!covered) {
                missing.add("no clause covers " + field + " = " + actual);
            }
        }
        return missing;
    }
}