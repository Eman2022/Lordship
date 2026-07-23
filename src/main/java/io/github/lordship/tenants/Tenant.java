package io.github.lordship.tenants;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record Tenant(
        UUID uuid,
        UUID tenancyId,
        UUID personId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    /*
    public boolean isSoftDeleted() {
        return deletedAt != null;
    } */
}
/*
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    tenancyId UUID NOT NULL,
    personId UUID NOT NULL,
    start_date DATE,
    end_date DATE,
    FOREIGN KEY (personId) REFERENCES personId(uuid),
    FOREIGN KEY (tenancyId) REFERENCES tenancyId(uuid) */