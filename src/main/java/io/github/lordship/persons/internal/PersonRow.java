package io.github.lordship.persons.internal;

import io.github.lordship.persons.Person;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PersonRow(
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
    public Person toPerson() {
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
                this.social,
                this.createdAt,
                this.deletedAt
        );
    }

    public PersonRow(String nameFirst, String nameLast, String personalEmail) {
        this(
                null,
                null,
                nameFirst,
                nameLast,
                null,
                null,
                personalEmail,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}