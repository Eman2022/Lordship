package io.github.lordship.lots.internal;

import io.github.lordship.lots.LotType;

public record LotTypeRow(
        String code,
        String label,
        String description,
        boolean active,
        Integer sortOrder
) {
    public LotType toLotType() {
        return new LotType(
                this.code,
                this.label,
                this.description,
                this.active,
                this.sortOrder
        );
    }
}