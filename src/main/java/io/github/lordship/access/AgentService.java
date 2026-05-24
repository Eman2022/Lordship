package io.github.lordship.access;


import io.github.lordship.access.internal.*;
import io.github.lordship.config.JwtService;
import io.github.lordship.persons.Person;
import io.github.lordship.persons.PersonService;
import io.github.lordship.persons.internal.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final PersonService personService;
    private final PasswordService passwordService;
    private final PermissionService permissionService;
    private final JwtService jwtService;

    public AgentService(
            AgentRepository agentRepository,
            PersonService personService,
            PasswordService passwordService,
            PermissionService permissionService,
            JwtService jwtService) {
        this.agentRepository = agentRepository;
        this.personService = personService;
        this.passwordService = passwordService;
        this.permissionService = permissionService;
        this.jwtService = jwtService;
    }

    @Transactional
    public AgentResponse registerAgent(AgentRegistrationRequest request){

        Person person = personService.createPerson(new PersonRow(
                request.nameFirst(),
                request.nameLast(),
                request.personalEmail()
        ));

        // hash the pass before it enters the DB
        String hashed = passwordService.hash(request.password());

        AgentRow agentRow = new AgentRow(
                person.uuid(),
                request.workPhone(),
                request.workEmail(),
                hashed
        );

        Agent agent = agentRepository.save(agentRow).toAgent();
        AgentWithPerson agentWithPerson = new AgentWithPerson(agent, person);

        String token = jwtService.generateToken(agentWithPerson, Set.of());
        return AgentResponse.from(agentWithPerson, token);
    }


    public Optional<AgentResponse> verifyLogin(String workEmail, String plainTextPassword){
        return findByWorkEmailForAuth(workEmail)
                .filter(row -> passwordService.verify(plainTextPassword, row.agentPassword()))
                .flatMap(row -> personService.findByID(row.personId())
                        .map(person -> {
                            AgentWithPerson agentWithPerson = new AgentWithPerson(row.toAgent(), person);
                            Set<Permission> permissions = permissionService.findPermissionsForAgent(row.uuid());
                            String token = jwtService.generateToken(agentWithPerson, permissions);
                            return AgentResponse.from(agentWithPerson, token);
                        })

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