package io.github.lordship.persons;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Person(
        UUID uuid,
        String nameFull,
        LocalDate birthday,
        String personalPhone,
        String personalEmail,
        String mailingAddress,
        UUID emergencyContact,
        String social,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}