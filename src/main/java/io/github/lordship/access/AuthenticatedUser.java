package io.github.lordship.access;

import java.util.UUID;


public record AuthenticatedUser (
        UUID id,
        String actorType
) {
}