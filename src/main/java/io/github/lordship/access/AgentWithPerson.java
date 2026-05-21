package io.github.lordship.access;

import io.github.lordship.persons.Person;

public record AgentWithPerson(
        Agent agent,
        Person person
) { }
