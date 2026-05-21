package io.github.lordship.persons;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record Person(
        UUID uuid,
        String nameRaw,
        String nameFirst,
        String nameLast,
        LocalDate birthday,
        String personalPhone,
        String personalEmail,
        String primaryProperty,
        String mailingAddress,
        UUID emergencyContact,
        String social,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public boolean isSoftDeleted() {
        return deletedAt != null;
    }

    public String fullName() {
        if (nameFirst != null && nameLast != null)
            return nameFirst + " " + nameLast;
        return nameRaw;
    }
}