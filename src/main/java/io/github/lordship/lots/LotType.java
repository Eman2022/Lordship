package io.github.lordship.lots;

public record LotType(
        String code,
        String label,
        String description,
        boolean active,
        Integer sortOrder
) {
}
