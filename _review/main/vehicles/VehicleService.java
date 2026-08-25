package io.github.lordship.vehicles;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.vehicles.internal.VehicleCreationResult;
import io.github.lordship.vehicles.internal.VehicleRepository;
import io.github.lordship.vehicles.internal.VehicleRow;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final AuditService auditService;

    public VehicleService(VehicleRepository vehicleRepository, AuditService auditService) {
        this.vehicleRepository = vehicleRepository;
        this.auditService = auditService;
    }

    @Transactional
    public VehicleCreationResult registerVehicle(UUID tenancyId, String plateNumber) {
        // Check for duplicate plate on same property under a different tenant (flag)
        List<Vehicle> conflicts = vehicleRepository
                .findUnregisteredByPlate(plateNumber, tenancyId)
                .stream().map(VehicleRow::toVehicle).toList();

        VehicleRow savedRow = vehicleRepository.save(tenancyId, plateNumber);
        auditService.recordInsert("vehicle", savedRow.uuid(), AuditMapper.toMap(savedRow));

        Vehicle saved = savedRow.toVehicle();

        return new VehicleCreationResult(saved, !conflicts.isEmpty(), conflicts);
    }

    public List<Vehicle> findByTenancy(UUID tenancyUuid) {
        return vehicleRepository.findByTenancy(tenancyUuid)
                .stream().map(VehicleRow::toVehicle).toList();
    }

    public List<Vehicle> findByProperty(UUID propertyId) {
        return vehicleRepository.findByProperty(propertyId)
                .stream().map(VehicleRow::toVehicle).toList();
    }

    public Optional<Vehicle> findById(UUID uuid) {
        return vehicleRepository.findById(uuid).map(VehicleRow::toVehicle);
    }

    @Transactional
    public Optional<Vehicle> patchVehicle(UUID uuid, Map<String, Object> changes) {
        Optional<VehicleRow> beforeOpt = vehicleRepository.findById(uuid);
        if (beforeOpt.isEmpty()) {
            return Optional.empty();
        }
        VehicleRow before = beforeOpt.get();

        Optional<VehicleRow> afterOpt = vehicleRepository.patch(uuid, changes);
        if (afterOpt.isEmpty()) {
            return Optional.empty();
        }
        VehicleRow after = afterOpt.get();

        var diff = AuditMapper.diff(before, after);
        if (!diff.before().isEmpty()) {
            auditService.recordUpdate("vehicle", uuid, diff.before(), diff.after());
        }

        return Optional.of(after.toVehicle());
    }

    @Transactional
    public boolean deleteVehicle(UUID uuid) {
        return vehicleRepository.findById(uuid).map(vehicle -> {
            if (!vehicleRepository.softDelete(uuid)) {
                return false;
            }
            auditService.recordDelete("vehicle", uuid, AuditMapper.toMap(vehicle));
            return true;
        }).orElse(false);
    }

}