package io.github.lordship.tenancy;


import io.github.lordship.tenancy.internal.TenancyRepository;
import io.github.lordship.tenancy.internal.TenancyRow;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@Transactional
public class TenancyService {
    private final TenancyRepository tenancyRepository;

    private static final Logger log = LoggerFactory.getLogger(TenancyService.class);


    public TenancyService(
            TenancyRepository tenancyRepository
    ) {
        this.tenancyRepository = tenancyRepository;
    }

    @Transactional
    public Tenancy create(Tenancy command) {
        List<TenancyRow> active = tenancyRepository.findActiveByLot(command.lotNumber());

        // temporary
        if (active.size() >= 1) {
            throw new RuntimeException("Lot already has an active tenancy");
        }

        Tenancy tenancy = tenancyRepository.save(TenancyRow.forInsert(
                command.lotNumber(),
                command.accountNumber(),
                command.startDate(),
                command.endDate()
        )).toTenancy();

        log.info("Tenancy created for lot {}", command.lotNumber());

        return tenancy;
    }

    public Optional<Tenancy> findTenancyById(UUID uuid) {
        return tenancyRepository.findById(uuid).map(TenancyRow::toTenancy);
    }

    public List<Tenancy> findActiveTenancyByLot(UUID lotNumber) {
        return tenancyRepository.findActiveByLot(lotNumber)
                .stream()
                .map(TenancyRow::toTenancy)
                .toList();
    }

    @Transactional
    public Tenancy endTenancy(UUID uuid, Date endDate) {
        TenancyRow row = tenancyRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Tenancy not found: " + uuid));

        if (row.endDate() != null) {
            throw new RuntimeException("Tenancy already closed: " + uuid);
        }

        Tenancy closed = tenancyRepository.close(uuid, endDate).toTenancy();

        log.info("Tenancy {} closed with end date {}", uuid, endDate);

        return closed;
    }

    @Transactional
    public void softDelete(UUID uuid) {
        tenancyRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Tenancy not found: " + uuid));

        tenancyRepository.softDelete(uuid);

        log.warn("Tenancy {} soft deleted", uuid);
    }
}
