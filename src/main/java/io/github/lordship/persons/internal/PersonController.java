package io.github.lordship.persons.internal;

import io.github.lordship.identity.LordshipPrincipal;
import io.github.lordship.persons.Person;
import io.github.lordship.persons.PersonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PreAuthorize("hasAuthority('persons:create')")
    @PostMapping("/create")
    public ResponseEntity<PersonResponse> createPerson(@Valid @RequestBody PersonCreateRequest createPersonRequest) {
        Person person = personService.createPersonFromName(createPersonRequest.nameFirst(), createPersonRequest.nameLast());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PersonResponse.from(person, true));
    }

    @PreAuthorize("hasAuthority('persons:view')")
    @GetMapping("/{uuid}")
    public ResponseEntity<PersonResponse> getPerson(@PathVariable UUID uuid, Authentication authentication) {

        boolean canViewSsn = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("persons_ssn:view"));

        return personService.findByID(uuid)
                .map(person -> ResponseEntity.ok(PersonResponse.from(person, canViewSsn)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('persons:view_own')")
    @GetMapping("/self")
    public ResponseEntity<PersonResponse> getSelf(Authentication authentication) {
        LordshipPrincipal principal = (LordshipPrincipal) authentication.getPrincipal();
        return personService.findByID(principal.personUuid())
                .map(person -> ResponseEntity.ok(PersonResponse.from(person, true)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('persons:edit')")
    @PatchMapping("/{uuid}")
    public ResponseEntity<PersonResponse> editPerson(
            @PathVariable UUID uuid,
            @Valid @RequestBody PersonPatchRequest request,
            Authentication authentication)
    {
        Map<String, Object> changes = new HashMap<>();
        request.nameRaw().ifPresent(v -> changes.put("name_raw", v));
        request.nameFirst().ifPresent(v -> changes.put("name_first", v));
        request.nameLast().ifPresent(v -> changes.put("name_last", v));
        request.birthday().ifPresent(v -> changes.put("birthday", v));
        request.personalEmail().ifPresent(v -> changes.put("personal_email", v));
        request.personalPhone().ifPresent(v -> changes.put("personal_phone", v));
        request.mailingAddress().ifPresent(v -> changes.put("mailing_address", v));
        request.emergencyContact().ifPresent(v -> changes.put("emergency_contact", v));
        request.social().ifPresent(v -> changes.put("social", v));

        boolean canViewSsn = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("persons_ssn:view"));

        return personService.patchPerson(uuid, changes)
                .map(person -> ResponseEntity.ok(PersonResponse.from(person, canViewSsn)))
                .orElse(ResponseEntity.notFound().build());
    }

}