package io.github.lordship.access.internal;

import io.github.lordship.access.AgentRegistrationRequest;
import io.github.lordship.access.AgentResponse;
import io.github.lordship.access.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/register")
    public ResponseEntity<AgentResponse> registerAgent(@Valid @RequestBody AgentRegistrationRequest request){
        AgentResponse agentResponse = agentService.registerAgent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(agentResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AgentResponse> login(@Valid @RequestBody AgentLoginRequest request){
        return agentService.verifyLogin(request.workEmail(), request.password())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

}
