package io.github.lordship.transactions.internal;

import io.github.lordship.transactions.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PreAuthorize("hasAuthority('transactions:edit')")
    @PostMapping("/post")
    public ResponseEntity<TransactionResponse> postTransaction(@Valid @RequestBody TransactionCreationRequest request) {
        try {
            TransactionResponse response = TransactionResponse.from(
                    transactionService.postTransaction(
                            request.accountId(),
                            request.type(),
                            request.amount(),
                            request.description(),
                            request.billingPeriod()
                    )
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PreAuthorize("hasAuthority('transactions:view')")
    @GetMapping("/{uuid}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID uuid) {
        try {
            return ResponseEntity.ok(TransactionResponse.from(transactionService.findById(uuid)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasAuthority('transactions:view')")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getByAccount(@PathVariable UUID accountId) {
        List<TransactionResponse> responses = transactionService.findByAccountId(accountId)
                .stream()
                .map(TransactionResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAuthority('transactions:edit')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable UUID uuid) {
        try {
            transactionService.deleteTransaction(uuid);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
