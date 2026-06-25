package io.github.lordship.tenancy;


import io.github.lordship.tenancy.internal.TenancyCreateRequest;
import io.github.lordship.tenancy.internal.TenancyRepository;
import io.github.lordship.tenancy.internal.TenancyRow;
import io.github.lordship.tenancy.internal.TenancyUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// todo: add springboot error handling
@Service
public class TenancyService {
    private final TenancyRepository tenancyRepository;

    private static final Logger log = LoggerFactory.getLogger(TenancyService.class);


    public TenancyService(
            TenancyRepository tenancyRepository
    ) {
        this.tenancyRepository = tenancyRepository;
    }

    @Transactional
    public Tenancy create(TenancyCreateRequest request) {
        List<TenancyRow> active = tenancyRepository.findActiveByLot(request.lotID());

        // temporary
        if (active.size() >= 1) {
            throw new IllegalStateException("Lot already has an active tenancy");
        }

        TenancyRow row = tenancyRepository.save(
                TenancyRow.forInsert(
                        request.lotID(),
                        LocalDate.now(),
                        null
                )
        );

        return row.toTenancy();
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
    public Tenancy endTenancy(UUID uuid, LocalDate endDate) {
        TenancyRow row = tenancyRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Tenancy not found: " + uuid));

        if (row.endDate() != null) {
            throw new RuntimeException("Tenancy already closed: " + uuid);
        }

        Tenancy closed = tenancyRepository.close(uuid, endDate).toTenancy();

        log.info("Tenancy {} closed with end date {}", uuid, endDate);

        return closed;
    }
/*
    @Transactional
    public Tenancy update(TenancyUpdateRequest request) {
        TenancyRow existing = tenancyRepository.findById(request.uuid())
                .orElseThrow(() -> new EntityNotFoundException("Tenancy not found"));

        return tenancyRepository.save(new Tenancy(
                existing.uuid(),
                request.lotID() != null ? request.lotID() : existing.lotID(),
                request.accountNumber() != null ? request.accountNumber() : existing.accountNumber(),
                request.startDate() != null ? request.startDate() : existing.startDate(),
                request.endDate() != null ? request.endDate() : existing.endDate(),
                request.status() != null ? request.status() : existing.status()
        ));
    }
*/
    @Transactional
    public void softDelete(UUID uuid) {
        tenancyRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Tenancy not found: " + uuid));

        tenancyRepository.softDelete(uuid);

        log.warn("Tenancy {} soft deleted", uuid);
    }
}
