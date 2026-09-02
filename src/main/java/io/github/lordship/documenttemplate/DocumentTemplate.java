package io.github.lordship.documenttemplate;

import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.InstrumentType;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * One document in the global pool -- "WA Land Lease 2026" -- as a packet of
 * sub-documents rather than a single sheet.
 *
 * <p>Global by definition: there are no property-scoped templates. A park picks
 * a document up through a document assignment, and an edit here reaches every
 * park that has. Documents already generated are untouched, because their
 * wording was snapshotted onto the instrument at generate.
 */
public record DocumentTemplate(
        UUID uuid,
        String name,
        AgreementType agreementType,
        InstrumentType instrumentType,
        int version,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt,
        List<DocumentSection> sections
) {
    public DocumentTemplate {
        sections = List.copyOf(sections);
    }

    public boolean isSoftDeleted() {
        return deletedAt != null;
    }

    /** Print order. Ordinals are sparse, so never assume they are 1..n. */
    public List<DocumentSection> sectionsInOrder() {
        return sections.stream()
                .sorted(Comparator.comparing(DocumentSection::ordinal))
                .toList();
    }

    /**
     * By key rather than uuid, because a key is stable across versions and a
     * uuid is not -- this is how "the septic addendum" stays findable after the
     * document has been re-authored.
     */
    public Optional<DocumentSection> section(String sectionKey) {
        return sections.stream()
                .filter(s -> sectionKey != null && sectionKey.equals(s.sectionKey()))
                .findFirst();
    }

    /** Every clause in the document, in print order, sections flattened. */
    public List<TemplateClause> clausesInOrder() {
        return sectionsInOrder().stream()
                .flatMap(section -> section.clausesInOrder().stream())
                .toList();
    }
}