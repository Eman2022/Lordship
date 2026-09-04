package io.github.lordship.documenttemplate;

import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.InstrumentType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Whether the documents a park is assigned can actually produce a complete
 * document for every deal in force there.
 *
 * <p>The check the other two cannot make. {@code TemplateClause.unguardedTokens}
 * asks whether a clause is honest about itself;
 * {@code DocumentSection.conditionCoverage} asks whether a section finished
 * what it started. Both look only at the template. This one looks at the
 * template against the deals that actually exist -- which is the only way to
 * find out that a park you set up differently from the others has been
 * generating leases with a paragraph missing.
 *
 * <p>An empty {@code findings} on every document is the answer you want.
 */
public record DocumentAudit (
        UUID propertyId,
        List<DocumentFinding> documents
) {

    /** One assigned document, measured against the configurations in force. */
    public record DocumentFinding(
            UUID assignmentUuid,
            UUID documentTemplateId,
            String documentName,
            int documentVersion,
            AgreementType agreementType,
            InstrumentType instrumentType,
            int configurationsChecked,
            List<Gap> gaps
    ) {
        public boolean complete() {
            return gaps.isEmpty();
        }
    }

    /**
     * One configuration whose lease would come out short, and what is missing.
     *
     * <p>{@code tenancyCount} is why this matters more than the template-only
     * checks: it says how many real tenancies would be handed the incomplete
     * document if someone generated today.
     */
    public record Gap(
            Map<String, String> methodValues,
            int tenancyCount,
            List<String> missing
    ) { }
}