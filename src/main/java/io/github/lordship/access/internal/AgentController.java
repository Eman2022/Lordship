package io.github.lordship.access.internal;

import io.github.lordship.access.*;
import io.github.lordship.identity.AgentPrincipal;
import io.github.lordship.persons.Person;
import io.github.lordship.persons.PersonService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PreAuthorize("hasAuthority('agents:create')")
    @PostMapping
    public ResponseEntity<AgentRegistrationResponse> registerAgent(@Valid @RequestBody AgentRegistrationRequest request){

        AgentWithPerson agentWithPerson = agentService.registerAgent(request.nameFull(), request.workPhone(), request.workEmail(), request.password());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AgentRegistrationResponse.from(agentWithPerson));
    }

    @PostMapping("/auth")
    public ResponseEntity<AgentLoginResponse> login(@Valid @RequestBody AgentLoginRequest request, HttpServletRequest httpRequest) {

        return agentService.verifyLogin(request.workEmail(),
                        request.password(),
                        httpRequest.getHeader("User-Agent"),
                        httpRequest.getRemoteAddr())
                .map(result -> AgentLoginResponse.from(result.agentWithPerson(), result.token()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }


    @PreAuthorize("hasAuthority('agents:reset_passwords')")
    @PutMapping("/{uuid}/password")
    public ResponseEntity<Void> changePassword(@PathVariable UUID uuid,
                                                  @Valid @RequestBody ChangePasswordRequest request){
        return agentService.setAgentPassword(uuid, request.newPassword())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }


}
