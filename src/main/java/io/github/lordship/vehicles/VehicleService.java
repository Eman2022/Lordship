package io.github.lordship.vehicles;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.vehicles.internal.VehicleCreateRequest;
import io.github.lordship.vehicles.internal.VehicleRepository;
import io.github.lordship.vehicles.internal.VehicleRow;
import io.github.lordship.vehicles.internal.VehiclePolicyRow;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    public VehicleRegistrationResult registerVehicle(VehicleCreateRequest request) {
        // Check for duplicate plate on same property under a different tenant (flag)
        List<Vehicle> conflicts = vehicleRepository
                .findUnregisteredByPlate(request.plateNumber(), request.tenancyUuid())
                .stream().map(VehicleRow::toVehicle).toList();

        // Count existing vehicles for this tenant
        int existingCount = vehicleRepository.countByTenancy(request.tenancyUuid());

        // Get property policy
        Optional<VehiclePolicy> policy = vehicleRepository
                .findPropertyUuidByTenancy(request.tenancyUuid())
                .flatMap(vehicleRepository::findPolicyByProperty)
                .map(VehiclePolicyRow::toPolicy);

        BigDecimal fee = BigDecimal.ZERO;
        if (policy.isPresent() && existingCount >= policy.get().freeVehicleLimit()) {
            fee = policy.get().extraVehicleFee();
        }

        VehicleRow row = new VehicleRow(
                request.tenancyUuid(),
                request.plateNumber()
        );

        VehicleRow savedRow = vehicleRepository.save(row);
        auditService.recordInsert("vehicle", savedRow.uuid(), AuditMapper.toMap(savedRow));

        Vehicle saved = savedRow.toVehicle();

        return new VehicleRegistrationResult(saved, fee, !conflicts.isEmpty(), conflicts);
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
            vehicleRepository.softDelete(uuid);
            auditService.recordDelete("vehicle", uuid, AuditMapper.toMap(vehicle));
            return true;
        }).orElse(false);
    }

    @Transactional
    public VehiclePolicy setPolicy(UUID propertyUuid, int freeLimit, BigDecimal fee, String notes) {
        Optional<VehiclePolicyRow> before = vehicleRepository.findPolicyByProperty(propertyUuid);

        VehiclePolicyRow row = new VehiclePolicyRow(null, propertyUuid, freeLimit, fee, notes, null, null);
        VehiclePolicyRow after = vehicleRepository.savePolicy(row);

        if (before.isPresent()) {
            var diff = AuditMapper.diff(before.get(), after);
            if (!diff.before().isEmpty()) {
                auditService.recordUpdate("vehicle_policy", after.uuid(), diff.before(), diff.after());
            }
        } else {
            auditService.recordInsert("vehicle_policy", after.uuid(), AuditMapper.toMap(after));
        }

        return after.toPolicy();
    }

    public Optional<VehiclePolicy> getPolicy(UUID propertyUuid) {
        return vehicleRepository.findPolicyByProperty(propertyUuid).map(VehiclePolicyRow::toPolicy);
    }
}