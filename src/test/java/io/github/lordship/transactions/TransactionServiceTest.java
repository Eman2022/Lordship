package io.github.lordship.transactions;

import io.github.lordship.accounts.AccountService;
import io.github.lordship.accounts.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TransactionServiceTest {

    @Autowired
    TransactionService transactionService;

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

    private Account createTestAccount() {
        UUID tenancyId = setupFullChain();
        return accountService.createAccount(tenancyId, null);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void postTransaction_returnsTransactionWithCorrectFields() {
        Account account = createTestAccount();
        LocalDate billingPeriod = LocalDate.of(2026, 1, 1);

        Transaction tx = transactionService.postTransaction(
                account.uuid(),
                TransactionType.CHARGE,
                new BigDecimal("150.00"),
                "Rent charge",
                billingPeriod
        );

        assertNotNull(tx.uuid());
        assertEquals(account.uuid(), tx.accountId());
        assertEquals(TransactionType.CHARGE, tx.type());
        assertEquals(0, new BigDecimal("150.00").compareTo(tx.amount()));
        assertEquals("Rent charge", tx.description());
        assertEquals(billingPeriod, tx.billingPeriod());
        assertFalse(tx.billed());
        assertNull(tx.deletedAt());
    }

    @Test
    void findById_returnsPostedTransaction() {
        Account account = createTestAccount();

        Transaction posted = transactionService.postTransaction(
                account.uuid(), TransactionType.CHARGE, new BigDecimal("200.00"), null, LocalDate.now()
        );

        Transaction found = transactionService.findById(posted.uuid());

        assertEquals(posted.uuid(), found.uuid());
        assertEquals(TransactionType.CHARGE, found.type());
        assertEquals(0, new BigDecimal("200.00").compareTo(found.amount()));
    }

    @Test
    void findById_throwsWhenNotFound() {
        assertThrows(NoSuchElementException.class, () ->
                transactionService.findById(UUID.randomUUID())
        );
    }

    @Test
    void findByAccountId_returnsAllTransactionsForAccount() {
        Account account = createTestAccount();

        transactionService.postTransaction(account.uuid(), TransactionType.CHARGE, new BigDecimal("100.00"), "Charge 1", LocalDate.now());
        transactionService.postTransaction(account.uuid(), TransactionType.PAYMENT, new BigDecimal("50.00"), "Payment 1", LocalDate.now());

        List<Transaction> transactions = transactionService.findByAccountId(account.uuid());

        assertEquals(2, transactions.size());
    }

    @Test
    void findByAccountId_returnsEmptyListForAccountWithNoTransactions() {
        Account account = createTestAccount();

        List<Transaction> transactions = transactionService.findByAccountId(account.uuid());

        assertTrue(transactions.isEmpty());
    }

    @Test
    void deleteTransaction_softDeletesMakesTransactionUnfindable() {
        Account account = createTestAccount();

        Transaction tx = transactionService.postTransaction(
                account.uuid(), TransactionType.CHARGE, new BigDecimal("75.00"), null, LocalDate.now()
        );

        transactionService.deleteTransaction(tx.uuid());

        assertThrows(NoSuchElementException.class, () ->
                transactionService.findById(tx.uuid())
        );
    }

    @Test
    void deleteTransaction_throwsWhenBilled() {
        Account account = createTestAccount();

        Transaction tx = transactionService.postTransaction(
                account.uuid(), TransactionType.CHARGE, new BigDecimal("75.00"), null, LocalDate.now()
        );

        // Mark as billed directly via JDBC (no service method for billing)
        jdbc.sql("UPDATE transaction SET billed = true WHERE uuid = :uuid")
                .param("uuid", tx.uuid())
                .update();

        assertThrows(IllegalStateException.class, () ->
                transactionService.deleteTransaction(tx.uuid())
        );
    }

    @Test
    void computeBalance_correctlySumsChargesMinusCredits() {
        Account account = createTestAccount();

        transactionService.postTransaction(account.uuid(), TransactionType.CHARGE, new BigDecimal("300.00"), null, LocalDate.now());
        transactionService.postTransaction(account.uuid(), TransactionType.PAYMENT, new BigDecimal("100.00"), null, LocalDate.now());

        BigDecimal balance = transactionService.computeBalance(account.uuid());

        // 300 (charge) - 100 (payment) = 200
        assertEquals(0, new BigDecimal("200.00").compareTo(balance));
    }

    @Test
    void computeBalance_returnsZeroForAccountWithNoTransactions() {
        Account account = createTestAccount();

        BigDecimal balance = transactionService.computeBalance(account.uuid());

        assertEquals(0, BigDecimal.ZERO.compareTo(balance));
    }
}
