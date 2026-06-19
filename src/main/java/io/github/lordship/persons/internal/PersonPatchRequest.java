package io.github.lordship.persons.internal;


import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public record PersonPatchRequest(

        Optional<String> nameRaw,

        Optional<String> nameFirst,

        Optional<String> nameLast,

        Optional<LocalDate> birthday,

        Optional<String> personalPhone,

        Optional<String> personalEmail,

        Optional<String> primaryProperty,

        Optional<String> mailingAddress,

        Optional<UUID> emergencyContact,

        Optional<String> social
) { }