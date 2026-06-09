package io.github.lordship.access;


import io.github.lordship.access.internal.*;
import io.github.lordship.persons.Person;
import io.github.lordship.persons.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final PersonService personService;
    private final PasswordService passwordService;
    private final PermissionResolverService permissionResolverService;
    private final JwtService jwtService;
    private final RoleAssignmentService roleAssignmentService;
    private static final Logger log = LoggerFactory.getLogger(AgentService.class);


    public AgentService(
            AgentRepository agentRepository,
            PersonService personService,
            PasswordService passwordService,
            PermissionResolverService permissionResolverService,
            JwtService jwtService, RoleAssignmentService roleAssignmentService) {
        this.agentRepository = agentRepository;
        this.personService = personService;
        this.passwordService = passwordService;
        this.permissionResolverService = permissionResolverService;
        this.jwtService = jwtService;
        this.roleAssignmentService = roleAssignmentService;
    }

    @Transactional
    public AgentRegistrationResponse registerAgent(AgentRegistrationRequest request){

        Person person = personService.createPersonFromName(request.nameFirst(), request.nameLast());

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

        return AgentRegistrationResponse.from(agentWithPerson);
    }


    public Optional<AgentLoginResponse> verifyLogin(String workEmail, String plainTextPassword){
        return findByWorkEmailForAuth(workEmail)
                .filter(row -> passwordService.verify(plainTextPassword, row.agentPassword()))
                .flatMap(row -> personService.findByID(row.personId())
                        .map(person -> {
                            AgentWithPerson agentWithPerson = new AgentWithPerson(row.toAgent(), person);
                            Set<Permission> permissions = permissionResolverService.findPermissionsForAgent(row.uuid());
                            String token = jwtService.generateToken(agentWithPerson, permissions);
                            return AgentLoginResponse.from(agentWithPerson, token);
                        })

                );
    }

    public void ensureRootAgentExists(String email, String password) {
        if (findByWorkEmail(email).isPresent()) return;

        AgentRegistrationRequest arr = new AgentRegistrationRequest("Root", "Admin", "", email, "", password);
        AgentRegistrationResponse resp = registerAgent(arr);

        roleAssignmentService.grantRoleByName(resp.uuid(), "Admin", resp.uuid());

        log.warn("Root agent with email {} has been created - change password ASAP", email);
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