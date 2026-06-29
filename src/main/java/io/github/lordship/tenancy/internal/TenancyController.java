package io.github.lordship.tenancy.internal;

import io.github.lordship.tenancy.Tenancy;
import io.github.lordship.tenancy.TenancyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// refer to person controller for structure

@Validated
@RestController
@RequestMapping("/tenancy")
public class TenancyController {

    private final TenancyService tenancyService;

    public TenancyController(TenancyService tenancyService) {
        this.tenancyService = tenancyService;
    }

    // Instead of ResponseEntity<Tenancy> we should send bare minimum info
    @PreAuthorize("hasAuthority('tenancy:create')")
    @PostMapping("/create")
    public TenancyResponse createTenancy(@RequestBody @Valid TenancyCreateRequest request) {
        return TenancyResponse.from(tenancyService.create(request));
    }

    // may not actually use "Tenancy"
    @PreAuthorize("hasAuthority('tenancy:delete')")
    @DeleteMapping("/{uuid}") // "/delete"?
    public void deleteTenancy(@PathVariable UUID uuid) {
        tenancyService.softDelete(uuid);
    }
}


