package io.github.lordship.persons.internal;

import io.github.lordship.persons.Person;
import io.github.lordship.shared.EncryptionService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;


//TODO: rule: row layer owns encryption for typed constructors. service layer owns it for dynamic/patch paths on sensitive fields.
public record PersonRow(
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
    LocalDateTime createdAt,
    LocalDateTime deletedAt
) {
    public Person toPerson(EncryptionService encryptionService) {
        return new Person(
                this.uuid,
                this.nameRaw,
                this.nameFirst,
                this.nameLast,
                this.birthday,
                this.personalPhone,
                this.personalEmail,
                this.primaryProperty,
                this.mailingAddress,
                this.emergencyContact,
                social != null ? encryptionService.decrypt(social) : null,
                this.createdAt,
                this.deletedAt
        );
    }

    public PersonRow(String nameFirst, String nameLast) {
        this(
                null,
                null,
                nameFirst,
                nameLast,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}