package io.github.lordship.persons;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.persons.internal.PersonPatchRequest;
import io.github.lordship.persons.internal.PersonRepository;
import io.github.lordship.persons.internal.PersonRow;
import io.github.lordship.shared.EncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PersonService {

    // NOTE: spring injects this repository
    private final PersonRepository personRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;

    public PersonService(PersonRepository personRepository,
                         EncryptionService encryptionService,
                         AuditService auditService) {
        this.personRepository = personRepository;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
    }


    @Transactional
    public Person createPersonFromName(String firstName, String lastName) {
        PersonRow row = personRepository.save(new PersonRow(firstName, lastName));
        auditService.recordInsert("person", row.uuid(), AuditMapper.toMap(row));
        return row.toPerson(encryptionService);
    }

    @Transactional
    public Person systemInsertRootPerson(String firstName, String lastName, UUID correlationId) {
        PersonRow row = personRepository.save(new PersonRow(firstName, lastName));
        auditService.recordSystemInsert(correlationId, "person", row.uuid(), AuditMapper.toMap(row));
        return row.toPerson(encryptionService);
    }

    public Optional<Person> findByID(UUID uuid) {
        return personRepository.findById(uuid)
                .map(row -> row.toPerson(encryptionService));
    }

    public Optional<Person> findByEmail(String email){
        return personRepository.findByEmail(email)
                .map(row -> row.toPerson(encryptionService));
    }

    @Transactional
    public Optional<Person> patchPerson(UUID uuid, Map<String, Object> changes) {
        if (changes.containsKey("social")) {
            changes.put("social", encryptionService.encrypt((String) changes.get("social")));
        }
        return personRepository.patch(uuid, changes)
                .map(row -> row.toPerson(encryptionService));
    }



}
