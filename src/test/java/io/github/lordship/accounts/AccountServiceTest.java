package io.github.lordship.accounts;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AccountServiceTest {

    @Autowired
    AccountService accountService;

    @Autowired
    JdbcClient jdbc;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID insertTestProperty() {
        return jdbc.sql("""
                INSERT INTO property (property_code, property_name, property_address)
                VALUES ('TST01', 'Test Mobile Park', '999 Test Ave') RETURNING uuid
                """)
                .query(UUID.class)
                .single();
    }

    private UUID insertTestLot(UUID propertyId) {
        return jdbc.sql("""
                INSERT INTO lot (property_id, lot_number)
                VALUES (:propertyId, '1')
                RETURNING uuid
                """)
                .param("propertyId", propertyId)
                .query(UUID.class)
                .single();
    }

    private UUID insertTestTenancy(UUID lotId) {
        return jdbc.sql("""
                INSERT INTO tenancy (lot_number, account_number, start_date)
                VALUES (:lotId, :placeholderAccountId, CURRENT_DATE)
                RETURNING uuid
                """)
                .param("lotId", lotId)
                .param("placeholderAccountId", UUID.randomUUID())
                .query(UUID.class)
                .single();
    }

    private UUID setupFullChain() {
        UUID propertyId = insertTestProperty();
        UUID lotId = insertTestLot(propertyId);
        return insertTestTenancy(lotId);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void createAccount_returnsActiveAccountWithDefaults() {
        UUID tenancyId = setupFullChain();

        Account account = accountService.createAccount(tenancyId, "test notes");

        assertNotNull(account.uuid());
        assertEquals(tenancyId, account.tenancyId());
        assertEquals(AccountStatus.ACTIVE, account.accountStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(account.balance()));
        assertFalse(account.autopayEnabled());
        assertFalse(account.noPersonalChecks());
        assertFalse(account.noPartialPayments());
        assertFalse(account.evictionInProgress());
        assertEquals("test notes", account.notes());
    }

    @Test
    void getAccount_returnsAccountWhenExists() {
        UUID tenancyId = setupFullChain();
        Account created = accountService.createAccount(tenancyId, null);

        Optional<Account> found = accountService.getAccount(created.uuid());

        assertTrue(found.isPresent());
        assertEquals(created.uuid(), found.get().uuid());
        assertEquals(AccountStatus.ACTIVE, found.get().accountStatus());
    }

    @Test
    void getAccount_returnsEmptyWhenNotExists() {
        Optional<Account> found = accountService.getAccount(UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    @Test
    void updateAccount_updatesCorrectFields() {
        UUID tenancyId = setupFullChain();
        Account created = accountService.createAccount(tenancyId, null);

        Account toUpdate = new Account(
                created.uuid(),
                created.tenancyId(),
                AccountStatus.DELINQUENT,
                created.balance(),
                true,
                "Late on payment",
                true,
                false,
                true,
                created.createdAt(),
                created.deletedAt()
        );

        Optional<Account> updated = accountService.updateAccount(toUpdate);

        assertTrue(updated.isPresent());
        assertEquals(AccountStatus.DELINQUENT, updated.get().accountStatus());
        assertTrue(updated.get().autopayEnabled());
        assertEquals("Late on payment", updated.get().notes());
        assertTrue(updated.get().noPersonalChecks());
        assertFalse(updated.get().noPartialPayments());
        assertTrue(updated.get().evictionInProgress());
    }

    @Test
    void updateAccount_doesNotChangeBalance() {
        UUID tenancyId = setupFullChain();
        Account created = accountService.createAccount(tenancyId, null);
        assertEquals(0, BigDecimal.ZERO.compareTo(created.balance()));

        // Pass an account with a different balance — the update SQL does not touch the balance column
        Account toUpdate = new Account(
                created.uuid(),
                created.tenancyId(),
                AccountStatus.ACTIVE,
                new BigDecimal("500.00"),
                false,
                null,
                false,
                false,
                false,
                created.createdAt(),
                created.deletedAt()
        );

        Optional<Account> updated = accountService.updateAccount(toUpdate);

        assertTrue(updated.isPresent());
        assertEquals(0, BigDecimal.ZERO.compareTo(updated.get().balance()));
    }

    @Test
    void deactivateAccount_cannotBeFoundAfterDeletion() {
        UUID tenancyId = setupFullChain();
        Account created = accountService.createAccount(tenancyId, null);

        Optional<Account> deleted = accountService.deactivateAccount(created.uuid());
        assertTrue(deleted.isPresent());

        Optional<Account> found = accountService.getAccount(created.uuid());
        assertTrue(found.isEmpty());
    }

    @Test
    void getAccountByTenancyId_returnsCorrectAccount() {
        UUID tenancyId = setupFullChain();
        Account created = accountService.createAccount(tenancyId, null);

        Optional<Account> found = accountService.getAccountByTenancyId(tenancyId);

        assertTrue(found.isPresent());
        assertEquals(created.uuid(), found.get().uuid());
        assertEquals(tenancyId, found.get().tenancyId());
    }
}
