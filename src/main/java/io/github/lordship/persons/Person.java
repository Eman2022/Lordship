package io.github.lordship.persons;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record Person(
        UUID uuid,
        String nameRaw,
        String nameFull,
        LocalDate birthday,
        String personalPhone,
        String personalEmail,
        String mailingAddress,
        UUID emergencyContact,
        String social,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}