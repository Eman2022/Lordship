package io.github.lordship.persons.internal;

import io.github.lordship.persons.Person;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PersonResponse(
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
        LocalDateTime createdAt
) {

    public static PersonResponse from(Person person) {
        return from(person, false);
    }

    public static PersonResponse from(Person person, boolean canViewSsn) {
        return new PersonResponse(
                person.uuid(),
                person.nameRaw(),
                person.nameFirst(),
                person.nameLast(),
                person.birthday(),
                person.personalPhone(),
                person.personalEmail(),
                person.primaryProperty(),
                person.mailingAddress(),
                person.emergencyContact(),
                canViewSsn ? person.social() : maskSsn(person.social()),
                person.createdAt()
        );
    }

    private static String maskSsn(String social) {
        if (social == null) return null;
        if (social.isEmpty()) return social;
        return "***-**-" + social.substring(social.length() - 4);
    }
}
