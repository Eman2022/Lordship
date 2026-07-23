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
    public Person toPerson(EncryptionService encryptionService) {
        return new Person(
                this.uuid,
                this.nameRaw,
                this.nameFull,
                this.birthday,
                this.personalPhone,
                this.personalEmail,
                this.mailingAddress,
                this.emergencyContact,
                social != null ? encryptionService.decrypt(social) : null,
                this.createdAt,
                this.deletedAt
        );
    }

    public PersonRow(String nameFull) {
        this(
                null,
                null,
                nameFull,
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