package io.github.lordship.access;


import io.github.lordship.access.internal.AgentRegistrationRequest;
import io.github.lordship.access.internal.AgentRepository;
import io.github.lordship.access.internal.AgentRow;
import io.github.lordship.access.internal.PasswordService;
import io.github.lordship.persons.Person;
import io.github.lordship.persons.PersonService;
import io.github.lordship.persons.internal.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final PersonService personService;
    private final PasswordService passwordService;

    public AgentService(
            AgentRepository agentRepository,
            PersonService personService,
            PasswordService passwordService) {
        this.agentRepository = agentRepository;
        this.personService = personService;
        this.passwordService = passwordService;
    }

    @Transactional
    public AgentWithPerson registerAgent(AgentRegistrationRequest request){

        Person person = personService.createPerson(new PersonRow(
                request.nameFirst(),
                request.nameLast(),
                request.personalEmail()
        ));

        // hash the pass before it ever enters the DB
        String hashed = passwordService.hash(request.password());

        AgentRow agentRow = new AgentRow(
                person.uuid(),
                request.workPhone(),
                request.workEmail(),
                hashed
        );

        Agent agent = agentRepository.save(agentRow).toAgent();
        return new AgentWithPerson(agent, person);
    }

    public Optional<AgentWithPerson> verifyLogin(String workEmail, String plainTextPassword){
        return findByWorkEmailForAuth(workEmail)
                .filter(row -> passwordService.verify(plainTextPassword, row.agentPassword()))
                .flatMap(row -> personService.findByID(row.personId())
                        .map(person -> new AgentWithPerson(row.toAgent(), person))
                );
    }


    public Optional<Agent> findById(String uuid){
        return agentRepository.findById(uuid).map(AgentRow::toAgent);
    }

    public Optional<Agent> findByWorkEmail(String workEmail){
        return agentRepository.findByWorkEmail(workEmail).map(AgentRow::toAgent);
    }

    Optional<AgentRow> findByWorkEmailForAuth(String workEmail){
        return agentRepository.findByWorkEmail(workEmail);
    }

}