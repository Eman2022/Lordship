package io.github.lordship;

import io.github.lordship.lots.internal.LotRepository;
import io.github.lordship.lots.internal.LotRow;
import io.github.lordship.properties.internal.PropertyRepository;
import io.github.lordship.properties.internal.PropertyRow;
import io.github.lordship.tenancy.internal.TenancyRepository;
import io.github.lordship.tenancy.internal.TenancyRow;

import java.util.UUID;


public final class TestDataSupport {
    private final PropertyRepository propertyRepository;
    private final LotRepository lotRepository;
    private final TenancyRepository tenancyRepository;

    private TestDataSupport(PropertyRepository propertyRepository, LotRepository lotRepository, TenancyRepository tenancyRepository) {
        this.propertyRepository = propertyRepository;
        this.lotRepository = lotRepository;
        this.tenancyRepository = tenancyRepository;
    }

    public PropertyRow insertProperty(String propertyName, String propertyAddress, String propertyCode) {
        return propertyRepository.save(propertyName, propertyAddress, propertyCode);
    }

    public PropertyRow insertProperty(String propertyCode) {
        return insertProperty("Test Mobile Park", "123 Test Ave", propertyCode);
    }

    public LotRow insertLot(UUID propertyId, String lotNumber) {
        return lotRepository.save(propertyId, lotNumber);
    }

    public TenancyRow insertTenancy(UUID lotId) {
        return tenancyRepository.save(lotId);
    }

    public TenancyRow insertChainToTenancy(){
        PropertyRow pr = insertProperty("TP");
        LotRow lr = insertLot(pr.uuid(), "1");
        return tenancyRepository.save(lr.uuid());
    }



}
