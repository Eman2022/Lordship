package io.github.lordship;

import io.github.lordship.accounts.internal.AccountRepository;
import io.github.lordship.accounts.internal.AccountRow;
import io.github.lordship.lots.internal.LotRepository;
import io.github.lordship.lots.internal.LotRow;
import io.github.lordship.properties.internal.PropertyRepository;
import io.github.lordship.properties.internal.PropertyRow;
import io.github.lordship.tenancy.Tenancy;
import io.github.lordship.tenancy.TenancyService;
import io.github.lordship.tenancy.internal.TenancyRepository;
import io.github.lordship.tenancy.internal.TenancyRow;

import java.util.UUID;


public final class TestDataSupport {
    private final PropertyRepository propertyRepository;
    private final LotRepository lotRepository;
    private final TenancyRepository tenancyRepository;
    private final AccountRepository accountRepository;

    private TestDataSupport(PropertyRepository propertyRepository, LotRepository lotRepository, TenancyRepository tenancyRepository, AccountRepository accountRepository) {
        this.propertyRepository = propertyRepository;
        this.lotRepository = lotRepository;
        this.tenancyRepository = tenancyRepository;
        this.accountRepository = accountRepository;
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
        TenancyRow tr = tenancyRepository.save(lotId);
        accountRepository.save(new AccountRow(tr.uuid(), null));
        return tr;
    }

    public TenancyRow insertChainToTenancy(){
        PropertyRow pr = insertProperty("TP");
        LotRow lr = insertLot(pr.uuid(), "1");
        TenancyRow tr = tenancyRepository.save(lr.uuid());
        accountRepository.save(new AccountRow(tr.uuid(), null));
        return tr;
    }



}
