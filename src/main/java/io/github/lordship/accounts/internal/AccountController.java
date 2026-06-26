package io.github.lordship.accounts.internal;

import io.github.lordship.accounts.Account;
import io.github.lordship.accounts.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PreAuthorize("hasAuthority('accounts:edit')")
    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountCreationRequest request) {
        Account account = accountService.createAccount(request.tenancyId(), request.notes());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @PreAuthorize("hasAuthority('accounts:view')")
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return accountService.getAccount(id)
                .map(AccountResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('accounts:view')")
    @GetMapping("/property/{propertyCode}")
    public ResponseEntity<List<AccountResponse>> getAccountsByProperty(@PathVariable UUID propertyId) {
        List<AccountResponse> responses = accountService.getAccountsByProperty(propertyId)
                .stream()
                .map(AccountResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAuthority('accounts:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable UUID id, @Valid @RequestBody AccountUpdateRequest request) {
        return accountService.getAccount(id)
                .flatMap(existing -> {
                    Account toUpdate = new Account(
                            existing.uuid(),
                            existing.tenancyId(),
                            request.accountStatus(),
                            existing.balance(),
                            request.autopayEnabled(),
                            request.notes(),
                            request.noPersonalChecks(),
                            request.noPartialPayments(),
                            request.evictionInProgress(),
                            existing.createdAt(),
                            existing.deletedAt()
                    );
                    return accountService.updateAccount(toUpdate);
                })
                .map(AccountResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('accounts:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID id) {
        boolean deleted = accountService.deactivateAccount(id).isPresent();
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
