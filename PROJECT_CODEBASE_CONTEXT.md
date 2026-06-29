# PROJECT_CODEBASE_CONTEXT.md
# Lordship — Full Codebase Reference
# Generated: 2026-06-28 | Branch: martin/feature/account-flags-and-transactions
# Java 25 · Spring Boot 4 · Spring Modulith 2.0.6 · PostgreSQL · JdbcClient only

---

## STACK

- **Language**: Java 25
- **Framework**: Spring Boot 4.0, Spring Modulith 2.0.6
- **Database**: PostgreSQL on port 5442 (test) / env var (dev/prod)
- **DB Access**: JdbcClient raw SQL only — NO JPA, NO Hibernate, NO @Entity
- **Migrations**: Flyway (V1–V16)
- **Auth**: JWT stateless — permissions re-loaded from DB on every request
- **Encryption**: AES-256-GCM with rotating key scheme (SSN storage)
- **Currency**: BigDecimal everywhere
- **Primary Keys**: `uuidv7()` default on most tables; `gen_random_uuid()` on transaction
- **Build**: Gradle

---

## MODULITH RULES (non-negotiable)

- Top-level package = public API only: domain record, public `@Service`, public enums
- `internal/` sub-package = private: `@RestController`, `@Repository`, Row records, request/response DTOs
- Cross-module communication ONLY through public `@Service` — never inject another module's `@Repository` or `@RestController`
- `ModulithBoundaryTest` must pass before every PR

---

## THREE LAYER RULE

| Layer | What it holds |
|-------|--------------|
| Row record | Raw DB types (strings for enums, encrypted SSNs, etc.) |
| Service / domain record | Clean, decrypted, fully typed (enums, BigDecimal, LocalDate) |
| Response DTO | Masked based on caller permissions |

---

## CODING PATTERNS

- Raw SQL via `JdbcClient` only
- `RETURNING *` on every INSERT and UPDATE
- `paramSource(row)` for JdbcClient binding — camelCase fields map to snake_case columns automatically
- Soft deletes: `deleted_at TIMESTAMP`, `WHERE deleted_at IS NULL` on all queries
- `@PreAuthorize` on every controller endpoint
- `AuditService` called on every mutation (insert, update, soft delete)
- SLF4J logging on every mutation — diff-based on updates (log only what changed)
- Row records have: canonical constructor (full), insert constructor (omit uuid/timestamps), `toX()` factory method

---

## DIRECTORY TREE

```
src/
├── main/
│   ├── java/io/github/lordship/
│   │   ├── LordshipApplication.java
│   │   ├── access/
│   │   │   ├── Agent.java                         (public domain record)
│   │   │   ├── AgentLoginRequest.java             (public DTO)
│   │   │   ├── AgentService.java                  (public @Service)
│   │   │   └── internal/
│   │   │       ├── AgentController.java
│   │   │       ├── AgentRepository.java
│   │   │       ├── AgentRow.java
│   │   │       ├── AgentRegistrationRequest.java
│   │   │       └── AgentResponse.java
│   │   ├── accounts/
│   │   │   ├── Account.java                       (public domain record)
│   │   │   ├── AccountService.java                (public @Service)
│   │   │   ├── AccountStatus.java                 (public enum)
│   │   │   └── internal/
│   │   │       ├── AccountController.java
│   │   │       ├── AccountRepository.java
│   │   │       ├── AccountRow.java
│   │   │       ├── AccountCreationRequest.java
│   │   │       ├── AccountUpdateRequest.java
│   │   │       └── AccountResponse.java
│   │   ├── audit/
│   │   │   ├── AuditService.java                  (public @Service)
│   │   │   ├── AuditMapper.java                   (public utility)
│   │   │   └── internal/
│   │   │       ├── AuditRepository.java
│   │   │       └── AuditRow.java
│   │   ├── lots/
│   │   │   ├── Lot.java                           (public domain record)
│   │   │   ├── LotCreationRequest.java            (public DTO)
│   │   │   ├── LotService.java                    (public @Service)
│   │   │   └── internal/
│   │   │       ├── LotController.java
│   │   │       ├── LotRepository.java
│   │   │       ├── LotRow.java
│   │   │       └── LotResponse.java
│   │   ├── persons/
│   │   │   ├── Person.java                        (public domain record)
│   │   │   ├── PersonService.java                 (public @Service — AUDIT/LOG REFERENCE IMPL)
│   │   │   └── internal/
│   │   │       ├── PersonController.java
│   │   │       ├── PersonRepository.java
│   │   │       ├── PersonRow.java
│   │   │       ├── PersonCreationRequest.java
│   │   │       ├── PersonUpdateRequest.java
│   │   │       └── PersonResponse.java
│   │   ├── properties/
│   │   │   ├── Property.java                      (public domain record)
│   │   │   ├── PropertyService.java               (public @Service)
│   │   │   └── internal/
│   │   │       ├── PropertyController.java
│   │   │       ├── PropertyRepository.java
│   │   │       ├── PropertyRow.java
│   │   │       └── PropertyResponse.java
│   │   ├── tenancy/
│   │   │   ├── Tenancy.java                       (public domain record)
│   │   │   ├── TenancyService.java                (public @Service)
│   │   │   └── internal/
│   │   │       ├── TenancyController.java
│   │   │       ├── TenancyRepository.java
│   │   │       └── TenancyRow.java
│   │   └── transactions/
│   │       ├── Transaction.java                   (public domain record)
│   │       ├── TransactionService.java            (public @Service)
│   │       ├── TransactionType.java               (public enum)
│   │       └── internal/
│   │           ├── TransactionController.java
│   │           ├── TransactionRepository.java
│   │           ├── TransactionRow.java
│   │           ├── TransactionCreationRequest.java
│   │           └── TransactionResponse.java
│   └── resources/
│       ├── application.properties
│       ├── application-test.properties
│       └── db/migration/
│           ├── V1__properties_and_persons.sql
│           ├── V2__pets.sql
│           ├── V3__agents_and_access.sql
│           ├── V4__audit_log.sql
│           ├── V5__seed_permissions.sql
│           ├── V6__property_info.sql
│           ├── V7__lots.sql
│           ├── V8__tenancies.sql
│           ├── V9__accounts.sql
│           ├── V10__account_permissions.sql
│           ├── V11__account_flags.sql
│           ├── V12__transactions.sql
│           ├── V13__rename_balance_to_balance_cached.sql
│           ├── V14__rename_eviction_in_progress_to_accept_payments.sql
│           ├── V15__add_exempt_from_late_fees.sql
│           └── V16__alter_transaction_table.sql
└── test/
    └── java/io/github/lordship/
        ├── LordshipApplicationTests.java          (pre-existing failure — missing env vars, do not touch)
        ├── ModulithBoundaryTest.java
        ├── AccountCrudTest.java                   (MockMvc integration test)
        ├── accounts/
        │   └── AccountServiceTest.java
        └── transactions/
            └── TransactionServiceTest.java
```

---

## FLYWAY MIGRATION CHAIN (V1–V16)

### V9__accounts.sql
```sql
CREATE TABLE account (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    tenancy_id UUID NOT NULL,
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    balance NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    autopay_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (tenancy_id) REFERENCES tenancy(uuid)
);
CREATE UNIQUE INDEX uq_account_tenancy_active ON account (tenancy_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_account_tenancy_id ON account(tenancy_id) WHERE deleted_at IS NULL;
```

### V11__account_flags.sql
```sql
ALTER TABLE account
    ADD COLUMN no_personal_checks   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN no_partial_payments  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN eviction_in_progress BOOLEAN NOT NULL DEFAULT FALSE;
```

### V12__transactions.sql
```sql
CREATE TABLE transaction (
    uuid             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id       UUID NOT NULL REFERENCES account(uuid),
    type             VARCHAR(50) NOT NULL,
    amount           NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    description      TEXT,
    billing_period   DATE NOT NULL,
    posted_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    billed           BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP
);
CREATE INDEX idx_transaction_account_id ON transaction(account_id);
CREATE INDEX idx_transaction_billing_period ON transaction(billing_period);
```

### V13__rename_balance_to_balance_cached.sql
```sql
ALTER TABLE account RENAME COLUMN balance TO balance_cached;
```

### V14__rename_eviction_in_progress_to_accept_payments.sql
```sql
ALTER TABLE account RENAME COLUMN eviction_in_progress TO accept_payments;
ALTER TABLE account ALTER COLUMN accept_payments SET DEFAULT TRUE;
UPDATE account SET accept_payments = TRUE WHERE accept_payments = FALSE;
```

### V15__add_exempt_from_late_fees.sql
```sql
ALTER TABLE account ADD COLUMN exempt_from_late_fees BOOLEAN NOT NULL DEFAULT FALSE;
```

### V16__alter_transaction_table.sql
```sql
-- Drop old CHECK (amount > 0), replace with one that allows negative for BALANCE_ADJUSTMENT
ALTER TABLE transaction DROP CONSTRAINT transaction_amount_check;
ALTER TABLE transaction ADD CONSTRAINT transaction_amount_check
    CHECK (amount <> 0 AND (amount > 0 OR type = 'BALANCE_ADJUSTMENT'));

-- Remove billed column — immutability now enforced by billing period finalization
ALTER TABLE transaction DROP COLUMN billed;
```

**Effective account table schema after all migrations:**
```
uuid              UUID PK DEFAULT uuidv7()
tenancy_id        UUID NOT NULL FK → tenancy(uuid)
account_status    VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
balance_cached    NUMERIC(10,2) NOT NULL DEFAULT 0.00
autopay_enabled   BOOLEAN NOT NULL DEFAULT FALSE
notes             TEXT
no_personal_checks  BOOLEAN NOT NULL DEFAULT FALSE
no_partial_payments BOOLEAN NOT NULL DEFAULT FALSE
accept_payments   BOOLEAN NOT NULL DEFAULT TRUE
exempt_from_late_fees BOOLEAN NOT NULL DEFAULT FALSE
created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
deleted_at        TIMESTAMP
```

**Effective transaction table schema after all migrations:**
```
uuid            UUID PK DEFAULT gen_random_uuid()
account_id      UUID NOT NULL FK → account(uuid)
type            VARCHAR(50) NOT NULL
amount          NUMERIC(14,2) NOT NULL CHECK (amount <> 0 AND (amount > 0 OR type = 'BALANCE_ADJUSTMENT'))
description     TEXT
billing_period  DATE NOT NULL
posted_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
deleted_at      TIMESTAMP
```

---

## accounts/ MODULE

### Account.java (public domain record)
```java
public record Account(
        UUID uuid,
        UUID tenancyId,
        AccountStatus accountStatus,
        BigDecimal balanceCached,
        boolean autopayEnabled,
        String notes,
        boolean noPersonalChecks,
        boolean noPartialPayments,
        boolean acceptPayments,       // TRUE = accepts payments (default), FALSE = blocked (eviction)
        boolean exemptFromLateFees,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public boolean isSoftDeleted() { return deletedAt != null; }
}
```

### AccountStatus.java (public enum)
```java
public enum AccountStatus { ACTIVE, DELINQUENT, CLOSED }
```

### AccountRow.java (internal)
```java
public record AccountRow(
        UUID uuid, UUID tenancyId, String accountStatus,
        BigDecimal balanceCached, boolean autopayEnabled, String notes,
        boolean noPersonalChecks, boolean noPartialPayments,
        boolean acceptPayments, boolean exemptFromLateFees,
        LocalDateTime createdAt, LocalDateTime deletedAt
) {
    public Account toAccount() { ... }

    // Insert constructor — sets defaults: acceptPayments=true, exemptFromLateFees=false
    public AccountRow(UUID tenancyId, String notes) {
        this(null, tenancyId, AccountStatus.ACTIVE.name(), BigDecimal.ZERO,
             false, notes, false, false, true, false, null, null);
    }
}
```

### AccountRepository.java (internal)
- `save(row)` — INSERT with `RETURNING *`; does NOT set `balance_cached` (defaults 0)
- `findById(uuid)` — WHERE deleted_at IS NULL
- `findByTenancyId(tenancyId)` — WHERE deleted_at IS NULL
- `findActiveByPropertyId(propertyId)` — JOIN tenancy → lot → property
- `update(row)` — updates status, flags, notes; does NOT touch `balance_cached`
- `softDelete(uuid)` — sets deleted_at = CURRENT_TIMESTAMP, RETURNING *

### AccountService.java (public @Service)
```java
@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    // Constructor: AccountRepository + AuditService

    public Account createAccount(UUID tenancyId, String notes)
        // saves row, calls auditService.recordInsert(), logs uuid+tenancyId

    public Optional<Account> getAccount(UUID uuid)
    public Optional<Account> getAccountByTenancyId(UUID tenancyId)
    public List<Account> getAccountsByProperty(UUID propertyId)

    public Optional<Account> updateAccount(Account account)
        // fetches before, runs update, computes AuditMapper.diff(before, after),
        // calls auditService.recordUpdate() if diff non-empty, logs changed fields

    public Optional<Account> deactivateAccount(UUID uuid)
        // findById().flatMap(...), softDeletes, calls auditService.recordDelete(), logs
}
```

### AccountCreationRequest.java (internal)
```java
public record AccountCreationRequest(@NotNull UUID tenancyId, String notes) {}
```

### AccountUpdateRequest.java (internal)
```java
public record AccountUpdateRequest(
    @NotNull AccountStatus accountStatus,
    boolean autopayEnabled,
    String notes,
    boolean noPersonalChecks,
    boolean noPartialPayments,
    boolean acceptPayments,
    boolean exemptFromLateFees
) {}
```

### AccountResponse.java (internal)
```java
public record AccountResponse(
    UUID uuid, UUID tenancyId, String accountStatus,
    BigDecimal balanceCached, boolean autopayEnabled, String notes,
    boolean noPersonalChecks, boolean noPartialPayments,
    boolean acceptPayments, boolean exemptFromLateFees,
    LocalDateTime createdAt
) {
    public static AccountResponse from(Account account) { ... }
}
```

### AccountController.java (internal)
- `POST /accounts/create` — `@PreAuthorize("hasAuthority('accounts:edit')")` → 201
- `GET /accounts/{id}` — `@PreAuthorize("hasAuthority('accounts:view')")` → 200/404
- `GET /accounts/property/{propertyCode}` — `@PreAuthorize("hasAuthority('accounts:view')")` → 200
- `PUT /accounts/{id}` — `@PreAuthorize("hasAuthority('accounts:edit')")` → 200/404; fetches existing to preserve `balanceCached`
- `DELETE /accounts/{id}` — `@PreAuthorize("hasAuthority('accounts:edit')")` → 204/404

---

## transactions/ MODULE

### TransactionType.java (public enum)
```java
public enum TransactionType {
    RENT_CHARGE,
    UTILITY_POWER,
    UTILITY_TRASH,
    UTILITY_SEWER,
    UTILITY_WATER,
    VEHICLE_FEE,
    LATE_FEE,
    CREDIT,
    BALANCE_ADJUSTMENT,
    MISC_CHARGE,
    PAYMENT
}
// NOTE: UTILITY_CREDIT was removed. BALANCE_ADJUSTMENT is signed (can be negative).
// Manual types requiring description: CREDIT, BALANCE_ADJUSTMENT, MISC_CHARGE
```

### Transaction.java (public domain record)
```java
public record Transaction(
        UUID uuid,
        UUID accountId,
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDate billingPeriod,
        LocalDateTime postedAt,
        LocalDateTime deletedAt
) {}
// NOTE: billed field removed — immutability enforced by billing period finalization
```

### TransactionRow.java (internal)
```java
public record TransactionRow(
        UUID uuid, UUID accountId, String type, BigDecimal amount,
        String description, LocalDate billingPeriod,
        LocalDateTime postedAt, LocalDateTime deletedAt
) {
    public Transaction toTransaction() { ... }
    // Insert constructor: (accountId, type, amount, description, billingPeriod)
}
```

### TransactionRepository.java (internal)
- `save(row)` — INSERT (account_id, type, amount, description, billing_period) RETURNING *
- `findById(uuid)` — WHERE deleted_at IS NULL
- `findByAccountId(accountId)` — WHERE deleted_at IS NULL ORDER BY posted_at DESC
- `softDelete(uuid)` — SET deleted_at = CURRENT_TIMESTAMP RETURNING *
- `computeBalance(accountId)`:
```sql
SELECT COALESCE(SUM(
    CASE WHEN type IN ('CREDIT', 'PAYMENT')
         THEN -amount
         ELSE amount   -- BALANCE_ADJUSTMENT is stored signed; all others positive
    END
), 0)
FROM transaction
WHERE account_id = :accountId AND deleted_at IS NULL
```

### TransactionService.java (public @Service)
```java
@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    public Transaction postTransaction(UUID accountId, TransactionType type,
                                       BigDecimal amount, String description,
                                       LocalDate billingPeriod)
        // Validates: description required for CREDIT, BALANCE_ADJUSTMENT, MISC_CHARGE
        // Throws IllegalArgumentException if missing/blank
        // Logs after successful post

    public Transaction findById(UUID uuid)        // throws NoSuchElementException if not found
    public List<Transaction> findByAccountId(UUID accountId)

    public void deleteTransaction(UUID uuid)
        // Throws NoSuchElementException if not found
        // Throws IllegalStateException if billingPeriod is before current month
        //   (billing period finalization: LocalDate.now().withDayOfMonth(1))
        // Logs after successful soft delete

    public BigDecimal computeBalance(UUID accountId)
}
```

---

## audit/ MODULE (reference — do not modify)

### AuditService.java (public @Service)
```java
// Three methods used by other services:
auditService.recordInsert(String table, UUID entityId, Map<String, Object> after)
auditService.recordUpdate(String table, UUID entityId, Map<String, Object> before, Map<String, Object> after)
auditService.recordDelete(String table, UUID entityId, Map<String, Object> before)
```

### AuditMapper.java (public utility)
```java
// Convert any record to Map<String, Object>:
AuditMapper.toMap(record)

// Compute diff between two records (returns Diff with before/after maps of changed fields only):
AuditMapper.Diff diff = AuditMapper.diff(before, after);
diff.before()   // Map of old values for changed fields
diff.after()    // Map of new values for changed fields
```

**IMPORTANT**: `AuditContext` is `@Scope(WebApplicationContext.SCOPE_REQUEST)` — it is null outside an HTTP request context. In service-level tests (no MockMvc), you MUST add `@MockitoBean AuditService auditService` to avoid NPE.

---

## TEST SUITE

- **Databases**: lordship_test on port 5442, user test_db_admin, password admin123
- **Config**: `application-test.properties` with `@ActiveProfiles("test")`
- **Baseline**: 44 tests total, 1 pre-existing failure (`LordshipApplicationTests.contextLoads` — missing env vars, do not touch)
- **All tests use**: `@SpringBootTest`, `@ActiveProfiles("test")`, `@Transactional`, `@MockitoBean AuditService auditService`
- **Test setup pattern**: Use public service methods only (PropertyService, LotService, TenancyService, AccountService) — zero raw JdbcClient SQL in test setup

### AccountCrudTest.java
- `@AutoConfigureMockMvc` + MockMvc integration test
- Reads `${lordship.root.email}` and `${lordship.root.password}` from test properties for JWT login
- `setupFullChain()`: PropertyService → LotService → TenancyService (returns tenancyId)
- Tests: 5 × unauthorized → 403, 4 × authorized CRUD → 201/200/200/204

### AccountServiceTest.java
- Direct service calls, no HTTP
- `setupFullChain()`: PropertyService → LotService → TenancyService (returns tenancyId)
- Tests: createAccount defaults, getAccount found/not-found, updateAccount fields/balance-immutability, deactivateAccount soft-delete, getAccountByTenancyId

### TransactionServiceTest.java
- Direct service calls, no HTTP
- `createTestAccount()`: PropertyService → LotService → TenancyService → AccountService
- Tests: postTransaction fields, findById found/not-found, findByAccountId list/empty, deleteTransaction soft-delete/billing-period-closed, postTransaction description-required, computeBalance sum/zero/signed-adjustment

---

## KEY BUSINESS RULES

| Rule | Detail |
|------|--------|
| `acceptPayments` default | TRUE — flip to FALSE when eviction starts |
| `balanceCached` | Cached value, not source of truth. Real balance = `computeBalance()` |
| `exemptFromLateFees` | Default FALSE — billing engine must check before applying late fees |
| BALANCE_ADJUSTMENT | Signed amount stored as-is. Negative = credit effect, positive = charge effect |
| Description required | CREDIT, BALANCE_ADJUSTMENT, MISC_CHARGE — throw `IllegalArgumentException` if null/blank |
| Billing period finalization | Transactions in closed months (before current month) cannot be deleted — throw `IllegalStateException` |
| `UTILITY_CREDIT` | Removed entirely — do not add back |
| Soft deletes | All entities use `deleted_at`; queries filter `WHERE deleted_at IS NULL` |
| Audit on all mutations | `recordInsert` on create, `recordUpdate` on update (diff only), `recordDelete` on soft delete |
| Logging | Every mutation logged; updates log only changed field names |
