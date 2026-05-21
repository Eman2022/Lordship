package io.github.lordship.persons;

import io.github.lordship.persons.internal.PersonRepository;
import io.github.lordship.persons.internal.PersonRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PersonService {

    // NOTE: spring injects this repository
    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Transactional
    public Person createPerson(PersonRow row) {
        return personRepository.save(row).toPerson();
    }

    public Optional<Person> findByID(UUID uuid) {
        return personRepository.findById(uuid).map(PersonRow::toPerson);
    }

    public Optional<Person> findByEmail(String email){
        return personRepository.findByEmail(email).map(PersonRow::toPerson);
    }
}
