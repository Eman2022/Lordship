package io.github.lordship.tenancy.internal;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

public class TenantControllerIT {
    @Autowired
    TenantRepository tenantRepository;

    private TenantRow buildRow() {
        return TenantRow.forInsert(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now(),
                null
        );
    }
}
