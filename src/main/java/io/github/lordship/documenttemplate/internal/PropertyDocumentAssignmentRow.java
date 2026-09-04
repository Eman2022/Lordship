package io.github.lordship.documenttemplate.internal;

import io.github.lordship.documenttemplate.DocumentTemplate;
import io.github.lordship.documenttemplate.PropertyDocumentAssignment;
import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.InstrumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PropertyDocumentAssignmentRow(
        UUID uuid,
        UUID property,
        UUID documentTemplate,
        AgreementType agreementType,
        InstrumentType instrumentType,
        String note,
        OffsetDateTime createdAt,
        UUID createdBy,
        OffsetDateTime deletedAt
) {

    /** The template arrives as a summary -- an assignment list has no use for sixty clause bodies. */
    public PropertyDocumentAssignment toPropertyDocumentAssignment(DocumentTemplate document) {
        return new PropertyDocumentAssignment(
                this.uuid,
                this.property,
                this.agreementType,
                this.instrumentType,
                this.note,
                this.createdAt,
                this.deletedAt,
                document
        );
    }
}