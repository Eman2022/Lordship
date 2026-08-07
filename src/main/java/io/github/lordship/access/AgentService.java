package io.github.lordship.access;


import eu.bitwalker.useragentutils.UserAgent;
import io.github.lordship.access.internal.*;
import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.persons.Person;
import io.github.lordship.persons.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final PersonService personService;
    private final PasswordService passwordService;
    private final PermissionResolverService permissionResolverService;
    private final JwtService jwtService;
    private final GrantedRoleService grantedRoleService;
    private final LoginEventRepository loginEventRepository;
    private final AuditService auditService;

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);


    public AgentService(
            AgentRepository agentRepository,
            PersonService personService,
            PasswordService passwordService,
            PermissionResolverService permissionResolverService,
            JwtService jwtService,
            GrantedRoleService grantedRoleService,
            LoginEventRepository loginEventRepository,
            AuditService auditService
    ) {
        this.agentRepository = agentRepository;
        this.personService = personService;
        this.passwordService = passwordService;
        this.permissionResolverService = permissionResolverService;
        this.jwtService = jwtService;
        this.grantedRoleService = grantedRoleService;
        this.loginEventRepository = loginEventRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AgentWithPerson registerAgent(String nameFull, String workPhone, String workEmail, String plainTextPassword) {

        Person person = personService.createPersonFromName(nameFull);

        // hash the pass before it enters the DB
        String hashed = passwordService.hash(plainTextPassword);

        AgentRow agentRow = new AgentRow(
                person.uuid(),
                workPhone,
                workEmail,
                hashed
        );

        Agent agent = agentRepository.save(agentRow).toAgent();

        // log the change
        auditService.recordInsert("agent", agent.uuid(), AuditMapper.toMap(agent));

        return new AgentWithPerson(agent, person);
    }

    @Transactional
    public boolean setAgentPassword(UUID agentId, String plainTextPassword) {
        Optional<AgentRow> found = agentRepository.findById(agentId);
        if (found.isEmpty()) {
            return false;
        }

        String currentHash = found.get().agentPassword();
        boolean unchanged = currentHash != null
                && passwordService.verify(plainTextPassword, currentHash);
        if (unchanged) {
            return true;
        }

        int updated = agentRepository.updatePassword(agentId, passwordService.hash(plainTextPassword));
        if (updated == 0) {
            return false;
        }

        auditService.recordUpdate("agent", agentId,
                Map.of("agent_password", "[redacted]"),
                Map.of("agent_password", "[reset]"));
        return true;
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
                OffsetDateTime.now(ZoneOffset.UTC),
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

    @Transactional
    public void ensureRootAgentExists(String email, String password) {
        if (findByWorkEmail(email).isPresent()) return;

        UUID correlationId = UUID.randomUUID();

        Person person = personService.systemInsertRootPerson("Root Admin", correlationId);
        String hashed = passwordService.hash(password);

        AgentRow agentRow = new AgentRow(
                person.uuid(),
                null,
                email,
                hashed
        );

        Agent agent = agentRepository.save(agentRow).toAgent();
        GrantedRole grantedRole = grantedRoleService.grantRoleByName(agent.uuid(), "Admin", agent.uuid());

        // log the changes
        auditService.recordSystemInsert(correlationId,"agent", agent.uuid(), AuditMapper.toMap(agent));
        auditService.recordSystemInsert(correlationId,"granted_role", grantedRole.uuid(), AuditMapper.toMap(grantedRole));

        log.warn("Root agent with email {} has been created - change password ASAP", email);
    }

    public Optional<Agent> findById(UUID uuid) {
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