package io.github.lordship.vehicles;

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

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public VehicleRegistrationResult registerVehicle(VehicleCreateRequest request) {
        // Check for duplicate plate on same property under a different tenant (flag)
        List<Vehicle> conflicts = vehicleRepository
                .findUnregisteredByPlate(request.plateNumber(), request.propertyId(), request.tenancyUuid())
                .stream().map(VehicleRow::toVehicle).toList();

        // Count existing vehicles for this tenant
        int existingCount = vehicleRepository.countByTenancy(request.tenancyUuid());

        // Get property policy
        Optional<VehiclePolicy> policy = vehicleRepository
                .findPolicyByProperty(request.propertyId())
                .map(VehiclePolicyRow::toPolicy);

        BigDecimal fee = BigDecimal.ZERO;
        if (policy.isPresent() && existingCount >= policy.get().freeVehicleLimit()) {
            fee = policy.get().extraVehicleFee();
        }

        VehicleRow row = new VehicleRow(
                request.tenancyUuid(),
                request.propertyId(),
                request.make(),
                request.model(),
                request.year(),
                request.plateNumber(),
                request.plateState(),
                request.color(),
                request.notes()
        );

        Vehicle saved = vehicleRepository.save(row).toVehicle();

        return new VehicleRegistrationResult(saved, fee, !conflicts.isEmpty(), conflicts);
    }

    public List<Vehicle> findByTenancy(UUID tenancyUuid) {
        return vehicleRepository.findByTenancy(tenancyUuid)
                .stream().map(VehicleRow::toVehicle).toList();
    }

//    public List<Vehicle> findByPropertyCode(String propertyCode) {
//        return vehicleRepository.findByPropertyCode(propertyCode)
//                .stream().map(VehicleRow::toVehicle).toList();
//    }

    public List<Vehicle> findByProperty(UUID propertyId) {
        return vehicleRepository.findByProperty(propertyId)
                .stream().map(VehicleRow::toVehicle).toList();
    }

    public Optional<Vehicle> findById(UUID uuid) {
        return vehicleRepository.findById(uuid).map(VehicleRow::toVehicle);
    }

    @Transactional
    public Optional<Vehicle> patchVehicle(UUID uuid, Map<String, Object> changes) {
        return vehicleRepository.patch(uuid, changes).map(VehicleRow::toVehicle);
    }

    @Transactional
    public boolean deleteVehicle(UUID uuid) {
        return vehicleRepository.softDelete(uuid);
    }

    @Transactional
    public VehiclePolicy setPolicy(UUID propertyCode, int freeLimit, BigDecimal fee, String notes) {
        VehiclePolicyRow row = new VehiclePolicyRow(null, propertyCode, freeLimit, fee, notes, null, null);
        return vehicleRepository.savePolicy(row).toPolicy();
    }

    public Optional<VehiclePolicy> getPolicy(UUID propertyCode) {
        return vehicleRepository.findPolicyByProperty(propertyCode).map(VehiclePolicyRow::toPolicy);
    }
}
