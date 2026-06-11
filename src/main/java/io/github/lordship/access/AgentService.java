package io.github.lordship.access;


import eu.bitwalker.useragentutils.UserAgent;
import io.github.lordship.access.internal.*;
import io.github.lordship.persons.Person;
import io.github.lordship.persons.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final PersonService personService;
    private final PasswordService passwordService;
    private final PermissionResolverService permissionResolverService;
    private final JwtService jwtService;
    private final RoleAssignmentService roleAssignmentService;
    private final LoginEventRepository loginEventRepository;

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);


    public AgentService(
            AgentRepository agentRepository,
            PersonService personService,
            PasswordService passwordService,
            PermissionResolverService permissionResolverService,
            JwtService jwtService,
            RoleAssignmentService roleAssignmentService,
            LoginEventRepository loginEventRepository
    ) {
        this.agentRepository = agentRepository;
        this.personService = personService;
        this.passwordService = passwordService;
        this.permissionResolverService = permissionResolverService;
        this.jwtService = jwtService;
        this.roleAssignmentService = roleAssignmentService;
        this.loginEventRepository = loginEventRepository;
    }

    @Transactional
    public AgentWithPerson registerAgent(String nameFirst, String nameLast, String workPhone, String workEmail, String plainTextPassword) {

        Person person = personService.createPersonFromName(nameFirst, nameLast);

        // hash the pass before it enters the DB
        String hashed = passwordService.hash(plainTextPassword);

        AgentRow agentRow = new AgentRow(
                person.uuid(),
                workPhone,
                workEmail,
                hashed
        );

        Agent agent = agentRepository.save(agentRow).toAgent();
        return new AgentWithPerson(agent, person);
    }


    public Optional<AgentAuthResult> verifyLogin(String workEmail, String plainTextPassword, String userAgentHeader, String ipAddress){
        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentHeader);

        Optional<AgentRow> agentRow = findByWorkEmailForAuth(workEmail);

        if (agentRow.isEmpty()){
            return Optional.empty();
        }

        AgentRow ar = agentRow.get();
        boolean passwordCorrect = passwordService.verify(plainTextPassword, ar.agentPassword());

        LoginEventRow loginEventRow = new LoginEventRow(
                ar.uuid(),
                LocalDateTime.now(),
                ipAddress,
                userAgent.getOperatingSystem().getName(),
                userAgent.getBrowser().getName(),
                passwordCorrect ? HttpStatus.OK.value() : HttpStatus.UNAUTHORIZED.value()
        );

        loginEventRepository.save(loginEventRow);

        if (passwordCorrect){
            return personService.findByID(ar.personId())
                    .map(person -> {
                        AgentWithPerson agentWithPerson = new AgentWithPerson(ar.toAgent(), person);
                        Set<Permission> permissions = permissionResolverService.findPermissionsForAgent(ar.uuid());
                        String token = jwtService.generateToken(agentWithPerson, permissions);
                        return new AgentAuthResult(agentWithPerson, token);
                    });
        }
        return Optional.empty();
    }

    public void ensureRootAgentExists(String email, String password) {
        if (findByWorkEmail(email).isPresent()) return;

        AgentWithPerson agentWithPerson = registerAgent("Root", "Admin", null, email, password);
        roleAssignmentService.grantRoleByName(agentWithPerson.agent().uuid(), "Admin", agentWithPerson.agent().uuid());

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

    public List<LoginEventRow> getLoginEventsByAgentId(UUID agentId) {
        return loginEventRepository.getLoginEventsByAgentId(agentId);
    }
}