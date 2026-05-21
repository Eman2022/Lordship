package io.github.lordship.persons.internal;


import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PersonRepository {

    private final JdbcClient jdbc;

    public PersonRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PersonRow save(PersonRow row) {
        return jdbc.sql("""
            INSERT INTO person (
                name_raw, name_first, name_last,
                birthday, personal_phone, personal_email,
                primary_property, mailing_address,
                emergency_contact, social
            ) VALUES (
                :nameRaw, :nameFirst, :nameLast,
                :birthday, :personalPhone, :personalEmail,
                :primaryProperty, :mailingAddress, :emergencyContact,
                :social
            ) RETURNING *
            """)
                .paramSource(row)
                .query(PersonRow.class)
                .single();
    }

    public Optional<PersonRow> findById(UUID uuid){
        return jdbc.sql("SELECT * FROM person WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(PersonRow.class)
                .optional();
    }

    public Optional<PersonRow> findByEmail(String email){
        return jdbc.sql("SELECT * FROM person WHERE LOWER(personal_email) = LOWER(:email) AND deleted_at IS NULL")
                .param("email", email)
                .query(PersonRow.class)
                .optional();
    }
}