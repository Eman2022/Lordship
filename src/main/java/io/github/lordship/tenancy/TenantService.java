package io.github.lordship.tenancy;

import io.github.lordship.tenancy.internal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TenantService
{
    private final TenantRepository tenantRepository;

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    public TenantService(
            TenantRepository tenantRepository
    ) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public Tenant create(TenantCreateRequest request) {
        List<TenantRow> active = tenantRepository.findByTenancy(request.tenancyId());
        active.forEach(t -> tenantRepository.end(t.uuid(), LocalDate.now()));

        TenantRow row = tenantRepository.save(
                TenantRow.forInsert(
                        request.tenancyId(),
                        request.personId(),
                        LocalDate.now(),
                        null
                )
        );

        return row.toTenant();
    }
}
