package io.github.lordship.persons;

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

    public PersonService(PersonRepository personRepository,
                         EncryptionService encryptionService) {
        this.personRepository = personRepository;
        this.encryptionService = encryptionService;
    }


    @Transactional
    public Person createPersonFromName(String firstName, String lastName) {
        return personRepository.save(new PersonRow(firstName, lastName)).toPerson(encryptionService);
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
