package io.github.lordship.persons.internal;

import io.github.lordship.persons.Person;
import io.github.lordship.shared.SensitiveDataMasker;

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
        UUID primaryProperty,
        String mailingAddress,
        UUID emergencyContact,
        String social,
        LocalDateTime createdAt
) {

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
                canViewSsn ? person.social() : SensitiveDataMasker.maskSsn(person.social()),
                person.createdAt()
        );
    }

}
