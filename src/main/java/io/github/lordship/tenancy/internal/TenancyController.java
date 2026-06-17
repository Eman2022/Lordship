package io.github.lordship.tenancy.internal;

import io.github.lordship.tenancy.Tenancy;
import io.github.lordship.tenancy.TenancyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Null;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


// todo: Remember to add PreAuths!!!!
@RestController
@RequestMapping("/tenancy")
public class TenancyController {

    private final TenancyService tenancyService;

    TenancyController(TenancyService tenancyService) {
        this.tenancyService = tenancyService;
    }

    @PreAuthorize("hasAuthority('tenancy:create')")
    @PostMapping("/create")
    ResponseEntity<Tenancy> createTenancy(@RequestBody @Valid Tenancy newTenancyRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenancyService.create(newTenancyRequest));
    }
/*
    // may not actually use "Tenancy"
    @PreAuthorize("hasAuthority('tenancy:delete')")
    @DeleteMapping("/delete")
    ResponseEntity<Tenancy> deleteTenancy(@RequestParam(name = "uuid", required = true) String uuid) {
        return
    } */
}


