package io.github.lordship.lots;

public record LotType(
        String code,
        String label,
        String description,
        // account status -> code -> active tenancy
        // soft delete if not exist
        boolean active,
        Integer sortOrder
) {
}
