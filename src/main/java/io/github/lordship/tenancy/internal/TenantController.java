package io.github.lordship.tenancy.internal;

import io.github.lordship.tenancy.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/tenant")
public class TenantController {
    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    // Instead of ResponseEntity<Tenancy> we should send bare minimum info
    @PreAuthorize("hasAuthority('tenant:create')")
    @PostMapping("/create")
    public TenantResponse createTenant(@RequestBody @Valid TenantCreateRequest request) {
        return TenantResponse.from(tenantService.create(request));
    }
}
