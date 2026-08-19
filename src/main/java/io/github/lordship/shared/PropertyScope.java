package io.github.lordship.shared;

import java.util.Set;
import java.util.UUID;

// provides a scope to keep agents to editing their own properties
public sealed interface PropertyScope {
    record All() implements PropertyScope {}
    record Restricted(Set<UUID> propertyIds) implements PropertyScope {}

    default boolean isAll() { return this instanceof All; }

    default Set<UUID> idsOrEmpty() {
        return this instanceof Restricted(var propertyIds)
                ? propertyIds
                : Set.of();
    }

    default boolean allows(UUID propertyId) {
        return isAll() || idsOrEmpty().contains(propertyId);
    }
}