package io.github.lordship.lots;

public record LotType(
        String code,
        String label,
        String description,
        // Retired types stay in the table for historical lot references, but
        // inactive types are hidden from create/edit dropdowns.
        boolean active,
        Integer sortOrder
) {
}
