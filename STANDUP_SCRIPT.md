# STANDUP SCRIPT
# PR: martin/feature/account-flags-and-transactions
# Walkthrough script with line references — present in order

---

## OPENING — say this before opening any files

"Alright so this PR has six distinct pieces and they all build on each other, so I want to walk through them in the order I wrote them.

One — I moved `AccountCreationRequest` into the internal package where it belongs, because it was sitting in the public package even though nothing outside the module ever uses it.

Two — I removed `balance` from the PUT update endpoint entirely. Balance is a computed value that lives in the transaction ledger. It should never be directly settable through a PUT request — that would let anyone inject an arbitrary number into the ledger.

Three — I refactored `updateAccount` in the service to accept a full `Account` domain record instead of six or seven individual parameters. That was getting messy and it was about to get worse.

Four — I added three new boolean flags to the account table through a Flyway migration. These flags drive real business rules around payments and evictions.

Five — I wrote service-layer tests for the accounts package. Not controller tests, not MockMvc, actual business logic tests that call the service directly.

Six — I built the entire transactions module from scratch. New public package, new internal package, new migration, eight files total. Let me walk through all of it."

---

## STOP 1 — V11__account_flags.sql
### File: src/main/resources/db/migration/V11__account_flags.sql
### Lines: 7–10

"Open V11. This is an ALTER TABLE, not a new CREATE TABLE. The account table already existed from V9. I'm adding three columns to it.

**Line 8 — `no_personal_checks BOOLEAN NOT NULL DEFAULT FALSE`.**
This flag is for when management decides they no longer want to accept personal checks from a specific tenant — maybe they've bounced checks before, maybe there's a policy reason. When this flag is true, the payment intake layer is supposed to reject personal check payments for that account. Default is false, meaning checks are accepted by default for all new accounts.

**Line 9 — `no_partial_payments BOOLEAN NOT NULL DEFAULT FALSE`.**
This one says whether we accept partial rent payments from this tenant. Sometimes management wants to enforce full payment only — no splitting rent across multiple transactions. When this is true, the system should reject any payment that doesn't cover the full amount owed. Again, default false — partial payments are allowed unless explicitly restricted.

**Line 10 — `eviction_in_progress BOOLEAN NOT NULL DEFAULT FALSE`.**
This is the most important of the three. When eviction proceedings have started for a tenant, we need to stop accepting money from them. Accepting payment during eviction can legally complicate the process. When this flag is true, the transaction layer must block any incoming PAYMENT-type transaction for that account. The flag is stored and returned correctly right now — the enforcement logic in `postTransaction` is what gets wired up next sprint.

All three are `NOT NULL DEFAULT FALSE` — so every existing account in the database automatically gets false for all three when this migration runs. No backfill needed."

---

## STOP 2 — Account.java
### File: src/main/java/io/github/lordship/accounts/Account.java
### Lines: 7–23

"Now open Account.java — the public domain record. This is what the rest of the system talks to. When the transaction module eventually needs to check whether eviction is in progress before posting a payment, it's going to call `AccountService.getAccount()` and read off this record.

**Lines 7–19 — the full record definition.**
The original fields were `uuid`, `tenancyId`, `accountStatus`, `balance`, `autopayEnabled`, `notes`, `createdAt`, `deletedAt`. That's what existed before this PR.

**Lines 16, 17, 18 — the three new fields I added: `noPersonalChecks`, `noPartialPayments`, `evictionInProgress`.**
They sit between `notes` and `createdAt` in the record. That ordering matters because Java records use positional constructors, so every place in the codebase that constructs an `Account` directly had to be updated to include these three new fields in the right position. I updated AccountRow's `toAccount()` method, the controller's PUT handler, and the tests — all of which I'll show you.

**Line 21 — `isSoftDeleted()`.** This convenience method just checks if `deletedAt` is non-null. It was there before, I kept it unchanged."

---

## STOP 3 — AccountRow.java
### File: src/main/java/io/github/lordship/accounts/internal/AccountRow.java
### Lines: 10–53

"Now open AccountRow.java — the internal record that mirrors the database columns exactly.

**Lines 10–22 — the full record definition.**
Every field here corresponds directly to a column in the account table. The key thing to understand about `AccountRow` versus `Account` is the types. Look at line 13 — `String accountStatus`. In `Account.java` that field is `AccountStatus accountStatus` — the proper Java enum. The row keeps it as a raw String because that's what JdbcClient maps from the database. The conversion from String to enum happens in `toAccount()`.

**Lines 23–37 — the `toAccount()` factory method.**
This is where the raw row gets turned into the proper domain object. Line 27 — `AccountStatus.valueOf(this.accountStatus)` is the enum conversion. Lines 32, 33, 34 — the three new flags map straight across. There's no transformation on them because they're booleans on both sides.

**Lines 39–53 — the insert constructor.**
This is the compact constructor used when creating a brand new account. You pass in just `tenancyId` and `notes` — the two things we need from the caller. Everything else is defaulted. Line 43 — `AccountStatus.ACTIVE.name()` sets status to ACTIVE as a string. Line 44 — `BigDecimal.ZERO` for the initial balance. Lines 48, 49, 50 — the three new flags all set to `false`. Line 51 — `null` for createdAt, because the database sets that via `DEFAULT CURRENT_TIMESTAMP`. Line 52 — `null` for deletedAt, because this account isn't deleted. The actual values come back through `RETURNING *` in the INSERT statement."

---

## STOP 4 — AccountCreationRequest.java — the move
### File: src/main/java/io/github/lordship/accounts/internal/AccountCreationRequest.java
### Lines: 1–15

"Open AccountCreationRequest.java. Notice where it lives — `accounts/internal/`. Before this PR it was sitting at the top level of the `accounts/` package.

The Spring Modulith rule we follow is that the top-level package is the public API — things other modules are allowed to use. The internal package is private to the module. `AccountCreationRequest` is a POST body DTO that only `AccountController` ever touches, and `AccountController` lives in `internal/`. Nothing outside the accounts module would ever construct or import a creation request — it doesn't make sense for it to be public.

**Line 1 — `package io.github.lordship.accounts.internal`** — that's the change. It was `io.github.lordship.accounts` before.

**Lines 7–13 — the record itself** hasn't changed at all. Same two fields — `tenancyId` with `@NotNull` and an optional `notes` string. The content is identical, just the package is corrected.

When I moved it, the import in `AccountController` updated automatically because they're now in the same package. I ran the Modulith boundary test after this change and it still passes — the module structure is now more honest about what's public and what's internal."

---

## STOP 5 — AccountUpdateRequest.java — balance removed, flags added
### File: src/main/java/io/github/lordship/accounts/internal/AccountUpdateRequest.java
### Lines: 6–22

"Open AccountUpdateRequest.java.

Before this PR, this record had four fields: `accountStatus`, `balance`, `autopayEnabled`, `notes`. `balance` was right there, `@NotNull`, settable through a PUT request. That was wrong.

**Lines 6–22 — the current fields: `accountStatus`, `autopayEnabled`, `notes`, `noPersonalChecks`, `noPartialPayments`, `evictionInProgress`.** No balance. Six fields — the status, the autopay toggle, free-text notes, and the three new flags.

The reason balance has to go is about data integrity. Balance should be a reflection of what transactions say it is. If an agent can PUT `balance: 0.00` directly to clear someone's debt, or PUT `balance: 999999.99` to inject a fake charge, the ledger is meaningless. The only correct way to change what a tenant owes is to post a transaction. So I removed balance from this request entirely, and then I made sure the SQL in the repository doesn't touch the balance column either — I'll show you that in a moment.

**Lines 15, 17, 19 — the three new flag fields.** These ARE settable through the update endpoint, and intentionally so. A manager needs to be able to flip `evictionInProgress` to true through a PUT request. That's the whole point of these flags — they're administrative controls that authorized agents turn on and off."

---

## STOP 6 — AccountResponse.java — flags in the response
### File: src/main/java/io/github/lordship/accounts/internal/AccountResponse.java
### Lines: 9–34

"Open AccountResponse.java.

**Lines 9–20 — the response record definition.** You can see `balance` is still here in the response — line 14. We still return the balance to the caller, we just don't let them set it through the update. The three new flags are at lines 17, 18, 19 — `noPersonalChecks`, `noPartialPayments`, `evictionInProgress`. They need to be in the response so the front end and any downstream services know what state those flags are currently in. Specifically, the transaction layer needs to see `evictionInProgress` when it decides whether to accept a payment.

**Lines 21–33 — the `from(Account account)` static factory.**
Line 22 is just the method signature. Lines 23 through 33 — each field of the response is mapped from the Account domain record. Line 30, 31, 32 — the three new flags mapped straight across from the Account. This factory is a pure projection, no logic, just mapping. The controller calls `AccountResponse.from(account)` and that's what goes back over the wire."

---

## STOP 7 — AccountRepository.java — two critical SQL changes
### File: src/main/java/io/github/lordship/accounts/internal/AccountRepository.java

"Open AccountRepository.java. Two SQL blocks to look at here.

**Lines 19–30 — the `save` method.**
This is the INSERT that runs when a new account is created. Look at lines 21 and 22 — the column list. I added `no_personal_checks`, `no_partial_payments`, `eviction_in_progress` after the original columns. Then lines 23 and 24 — the corresponding named parameters: `:noPersonalChecks`, `:noPartialPayments`, `:evictionInProgress`. Notice how the Java field names are camelCase and the SQL parameter names are also camelCase — that's because `paramSource(row)` on line 27 handles the mapping automatically. Spring's JdbcClient reads the record component names from `AccountRow` and binds them to the `:paramName` placeholders by name. Line 25 — `RETURNING *`. Every INSERT in this codebase uses `RETURNING *` so we get back the full row immediately, including the database-generated `uuid`, `created_at`, and all the default values, without having to do a second SELECT.

**Lines 59–74 — the `update` method.**
This is the change that enforces balance immutability at the database layer. Lines 62–67 — the SET clause. You have `account_status`, `autopay_enabled`, `notes`, and the three new flags. Count carefully — there is no `balance` in this SET clause. Even if someone passed a balance in, the SQL will never write it to the database. This is intentional and it is a second, independent layer of protection on top of removing balance from `AccountUpdateRequest`. Line 68 — `WHERE uuid = :uuid AND deleted_at IS NULL` is the soft-delete guard. We will never update a soft-deleted account. Line 69 — `RETURNING *` again, so we get the full updated row back and can return it to the caller without a follow-up SELECT."

---

## STOP 8 — AccountService.java — the refactor
### File: src/main/java/io/github/lordship/accounts/AccountService.java
### Lines: 13–64

"Now the service. Open AccountService.java.

Before this PR, `updateAccount` had this signature: `updateAccount(UUID uuid, AccountStatus status, BigDecimal balance, boolean autopayEnabled, String notes)`. Five parameters, and that was already messy. With three new flags it was about to be eight. Long parameter lists are a maintenance problem — callers have to get the order exactly right, and it's hard to read at the call site.

**Lines 43–58 — the refactored `updateAccount`.** The method now takes a single `Account` domain record. The caller is responsible for building that Account with the right values. Lines 44–56 — I convert the Account back to an AccountRow by pulling all the fields out. The row is what `paramSource` in the repository needs. Line 57 — `accountRepository.update(row)` and then `.map(AccountRow::toAccount)` to convert the result back to a domain record. The Optional flows through — if the account doesn't exist or is already soft-deleted, the repository returns empty and we return empty to the controller, which returns 404.

**Lines 31–33 — `getAccountByTenancyId`.** This was already in the repository — `findByTenancyId` existed on `AccountRepository` — but it was never exposed on the service, which meant nothing could call it from outside the module. The transaction layer needs to look up an account by tenancy ID, so I wired this up. It's a one-liner: call the repository, map the row to a domain record, return Optional."

---

## STOP 9 — AccountController.java — how balance protection works at the controller layer
### File: src/main/java/io/github/lordship/accounts/internal/AccountController.java
### Lines: 50–73

"Open AccountController.java and go to line 50 — the PUT endpoint. This is where the refactor to accept a full Account record shows up most clearly.

**Line 52 — the method signature.** Takes the path variable `id` and a `@Valid @RequestBody AccountUpdateRequest`. Remember, that request has no balance field.

**Line 53 — `accountService.getAccount(id)`.** The first thing the controller does is fetch the existing account from the database. This gives us the current state of the account — including the current balance, the current timestamps, everything.

**Lines 54–67 — the `flatMap` block where we build the Account to update.** Line 55 — `new Account(...)`. Look at the arguments one by one.
- Line 56 — `existing.uuid()` — the ID we're updating.
- Line 57 — `existing.tenancyId()` — preserved from the existing account.
- Line 58 — `request.accountStatus()` — this comes from the request, the agent can change it.
- Line 59 — **`existing.balance()`** — this is the critical line. Balance comes from the existing account in the database, not from anywhere in the request. The request doesn't even have a balance field, but even if it did, this line would ignore it.
- Line 60 — `request.autopayEnabled()` — from the request.
- Line 61 — `request.notes()` — from the request.
- Lines 62, 63, 64 — the three new flags, all from the request. These are what a manager can actually change through this endpoint.
- Lines 65, 66 — `existing.createdAt()` and `existing.deletedAt()` preserved from the database.

**Line 68 — `accountService.updateAccount(toUpdate)`.** We pass the fully constructed Account to the service. The service converts it to a row and calls the repository. Because updateAccount returns `Optional<Account>` and we're inside a `flatMap`, the Optional chains cleanly.

So to be explicit: there are now three separate barriers protecting balance from being changed through a PUT request. The request DTO has no balance field. The controller explicitly uses `existing.balance()` when building the Account. And the repository's UPDATE SQL has no balance in the SET clause. You would have to break three independent things to accidentally let balance be overwritten."

---

## STOP 10 — AccountServiceTest.java — service-layer tests
### File: src/test/java/io/github/lordship/accounts/AccountServiceTest.java

"Now the tests. Open AccountServiceTest.java.

**Lines 16, 17, 18 — the annotations.** `@SpringBootTest` loads the full application context. `@ActiveProfiles("test")` points it at the test database on port 5442. `@Transactional` means every test rolls back after it runs — nothing persists between tests, no test pollution.

Notice what's NOT here — there's no `@AutoConfigureMockMvc`, no `MockMvc` field, no HTTP request building. These are not controller tests. This is testing the business logic in `AccountService` directly. When Erich said 'we need tests that target the service layer,' this is what that means — call the service methods, assert on what comes back, skip the HTTP layer entirely.

**Lines 21–25 — two autowired fields.** `AccountService` is the thing we're testing. `JdbcClient` is only here for test setup — I use it to insert the prerequisite data the tests need.

**Lines 31–67 — the setup helpers.** Because Account requires a Tenancy, which requires a Lot, which requires a Property, I need to insert all three before I can create an account in a test. These helpers — `insertTestProperty` at line 31, `insertTestLot` at line 40, `insertTestTenancy` at line 51 — use raw JdbcClient SQL to insert the data directly. `setupFullChain` at line 63 chains all three and returns the tenancyId that the test can then use to create an account.

**Lines 73–88 — `createAccount_returnsActiveAccountWithDefaults`.**
This test calls `accountService.createAccount(tenancyId, "test notes")` and then asserts on every single field of the returned Account. Line 81 — status is ACTIVE. Line 82 — balance is zero, using `BigDecimal.ZERO.compareTo()` because BigDecimal equality is scale-sensitive. Lines 83–86 — autopay is false, and all three new flags are false. Line 87 — the notes string came through correctly. This test is a contract for the insert constructor defaults. If someone changes the `AccountRow` insert constructor and accidentally defaults a flag to true, this test fails immediately.

**Lines 139–164 — `updateAccount_doesNotChangeBalance`.**
This is the most important test in the file. Line 142 — create an account, which starts with balance zero. Line 143 — assert it is in fact zero. Lines 146–158 — build a new `Account` record and put `new BigDecimal("500.00")` as the balance argument. Line 160 — call `accountService.updateAccount(toUpdate)`. Line 163 — assert balance is still zero. The 500 was passed all the way down to the service. The service converted it to an AccountRow. The repository ran the UPDATE SQL. But because balance is not in the SET clause, the database never touched it, and `RETURNING *` gave back the original zero. This test proves the invariant works end to end. If anyone ever adds balance back to the UPDATE SQL, this test fails with a clear message."

---

## STOP 11 — TransactionType.java
### File: src/main/java/io/github/lordship/transactions/TransactionType.java
### Lines: 3–16

"Now the transactions module. Everything from here is brand new — I built the whole thing from scratch this sprint.

Open TransactionType.java. Twelve values in the enum.

The grouping matters for how balance is calculated, so let me be explicit about it.

**Charge types — lines 4, 5, 6, 7, 8, 11, 12, 13:** `RENT_CHARGE`, `UTILITY_POWER`, `UTILITY_TRASH`, `UTILITY_SEWER`, `UTILITY_WATER`, `VEHICLE_FEE`, `LATE_FEE`, `MISC_CHARGE`. These represent money the tenant owes us. When `computeBalance` runs, these contribute a positive number to the total.

**Credit and payment types — lines 9, 10, 14, 15:** `UTILITY_CREDIT`, `CREDIT`, `BALANCE_ADJUSTMENT`, `PAYMENT`. These reduce what the tenant owes. When `computeBalance` runs, these are negated — their amount is subtracted from the total.

The reason all amounts are stored as positive in the database and the sign is applied at query time is so the check constraint `amount > 0` on the transaction table is simple and enforceable. We never store negative numbers. The business logic about which types are debits and which are credits lives in the SQL CASE expression in `computeBalance`. I'll show you that when we get to the repository."

---

## STOP 12 — Transaction.java
### File: src/main/java/io/github/lordship/transactions/Transaction.java
### Lines: 8–18

"Open Transaction.java — the public domain record for the transactions module.

**Lines 8–18 — the nine fields.**
`uuid` and `accountId` are the identity and the foreign key. `type` is the `TransactionType` enum — the proper typed enum, not a String. `amount` is BigDecimal — same as balance, always BigDecimal for money in this codebase, never double or float. `description` is optional free text. `billingPeriod` is a `LocalDate` — this is the period the charge applies to, like June 2026, which may be different from when it was actually posted. `postedAt` is a `LocalDateTime` — when it was entered into the system.

Then `billed` and `deletedAt` — these two fields together define the immutability and soft-delete behavior of a transaction.

`billed` being true means the transaction has been locked. It was included in a billing cycle that was sent out. At that point it cannot be deleted — you would be altering a bill that a tenant has already received. If you try to delete a billed transaction, the service throws `IllegalStateException` and the controller returns 409 Conflict.

`deletedAt` is in the domain record, not just the row, because downstream code may need to know if something was soft-deleted. For example, if you're displaying a transaction history that includes voided items, you'd filter on `deletedAt` at the application layer."

---

## STOP 13 — V12__transactions.sql
### File: src/main/resources/db/migration/V12__transactions.sql
### Lines: 7–20

"Open V12. This creates the transaction table and two indexes.

**Line 8 — `uuid UUID PRIMARY KEY DEFAULT gen_random_uuid()`.**
Most tables in this codebase use `uuidv7()` as the default — that's a time-ordered UUID. For transactions I used `gen_random_uuid()` instead. Transactions don't need to be globally time-sortable by UUID because we have `posted_at` and `billing_period` for ordering. So standard random UUID is fine here.

**Line 9 — `account_id UUID NOT NULL REFERENCES account(uuid)`.**
Hard FK. Every transaction must belong to a real, existing account. No orphaned transactions.

**Line 10 — `amount NUMERIC(14, 2) NOT NULL CHECK (amount > 0)`.**
Two things here. `NUMERIC(14, 2)` — 14 digits total, 2 decimal places, same precision class as the balance column. The `CHECK (amount > 0)` constraint enforces at the database level that you can never insert a zero or negative amount. Credits and payments are expressed as positive amounts with a type that implies the negative direction — `PAYMENT` with amount 500 means the tenant paid 500, not that we're storing -500.

**Line 11 — `billing_period DATE NOT NULL`.**
This is required. Every transaction has to be attributed to a billing period so that monthly statements can be generated. You can't post a charge without saying which month it belongs to.

**Lines 12–15 — `posted_at`, `billed`, `deleted_at`.**
`posted_at` defaults to the current timestamp — the database sets this when the row is inserted. `billed` defaults to false — every new transaction starts as unbilled. `deleted_at` is nullable — null means active, a timestamp means soft-deleted.

**Lines 17–18 — the two indexes.**
One on `account_id` because almost every query against the transaction table will be scoped to a specific account. One on `billing_period` because monthly reporting will group and filter by period. These aren't optional — without them, any account with hundreds of transactions would be doing full table scans."

---

## STOP 14 — TransactionService.java
### File: src/main/java/io/github/lordship/transactions/TransactionService.java
### Lines: 14–56

"Open TransactionService.java. This is the public service — the only thing other modules are allowed to call.

**Lines 23–28 — `postTransaction`.**
Five parameters: `accountId`, `type`, `amount`, `description`, `billingPeriod`. Line 26 — `new TransactionRow(accountId, type, amount, description, billingPeriod)` uses the insert constructor on the row record, which defaults `uuid`, `postedAt`, `billed`, and `deletedAt` to null or false. Line 27 — `transactionRepository.save(row)` runs the INSERT and returns the full row via `RETURNING *`. Then `.toTransaction()` converts it to the domain record. Simple and clean.

One thing I want to flag — there's a TODO here that isn't written yet. Before calling `save`, this method needs to check `AccountService.getAccount(accountId).evictionInProgress()` and throw an exception if the account is in eviction and the type is `PAYMENT`. That enforcement is the next sprint. The infrastructure is all here, it just needs that guard condition added.

**Lines 30–34 — `findById`.**
This returns `Transaction` directly, not `Optional<Transaction>`. I made that choice because the spec called for it and because it makes the contract explicit — either the transaction exists or we throw. Line 33 — `orElseThrow(() -> new NoSuchElementException(...))`. The controller catches `NoSuchElementException` and returns 404. This is a different pattern from `getAccount` which returns Optional — both are valid, this one just expresses 'not found is exceptional' rather than 'not found is a normal case.'

**Lines 43–51 — `deleteTransaction`.**
This is where billed immutability is enforced at the service layer. Line 45 — first we try to find the transaction. If it doesn't exist, `orElseThrow` fires and we get `NoSuchElementException` which becomes a 404. Line 47 — if it does exist, we check `row.billed()`. If it's true, line 48 throws `IllegalStateException` with a clear message. The controller catches that and returns 409 Conflict. If billed is false, line 50 — we call `transactionRepository.softDelete(uuid)`. We don't return anything here, the method is void — the controller just returns 204 No Content.

**Lines 53–55 — `computeBalance`.**
One line delegation to the repository. The service is thin here because there's no business logic on top of the SQL — the CASE expression in the repository does all the work. Other modules call this method to get the authoritative net balance for an account."

---

## STOP 15 — TransactionRepository.java — the computeBalance SQL
### File: src/main/java/io/github/lordship/transactions/internal/TransactionRepository.java
### Lines: 57–71

"Open TransactionRepository.java. Most of this file is standard — save, find, softDelete following the same patterns as AccountRepository. I want to spend time on `computeBalance` at line 57 because that's the most interesting SQL in the PR.

**Lines 58–70 — the full query.**
Line 59 — `SELECT COALESCE(SUM(...), 0)`. The COALESCE is there because if an account has zero non-deleted transactions, `SUM` returns NULL rather than 0. COALESCE converts that NULL to 0, which is the correct starting balance.

Lines 60–63 — the CASE expression inside SUM. `WHEN type IN ('CREDIT', 'UTILITY_CREDIT', 'PAYMENT', 'BALANCE_ADJUSTMENT') THEN -amount ELSE amount END`. This is where the sign logic lives. Every transaction in the database has a positive amount. The CASE expression negates the amount for credit and payment types so they reduce the total, and passes charges through as positive so they add to the total. The SUM then adds everything together and gives you the net balance.

Line 65 — `FROM transaction`. We're summing all transactions for this account.

Line 66 — `WHERE account_id = :accountId AND deleted_at IS NULL`. Two filters — scoped to the specific account, and excluding soft-deleted transactions. If a transaction was deleted before being billed, it should not count toward what the tenant owes. This WHERE clause enforces that.

The result is a single `BigDecimal` — the net amount owed by the tenant on this account right now."

---

## STOP 16 — TransactionController.java — four endpoints with the 409 pattern
### File: src/main/java/io/github/lordship/transactions/internal/TransactionController.java
### Lines: 14–71

"Open TransactionController.java.

**Lines 14–16 — the class setup.** `@RestController`, `@RequestMapping("/transactions")` — same pattern as AccountController.

**Lines 24–37 — `POST /transactions/post`.**
Line 24 — `@PreAuthorize("hasAuthority('accounts:edit')")`. Transactions use the existing accounts permissions — `accounts:edit` to post or delete, `accounts:view` to read. We reuse these because transactions are part of the account package conceptually. If the team decides we need dedicated transaction permissions, that's a V13 migration.
Line 26 — the method takes a `@Valid @RequestBody TransactionCreationRequest`. Lines 27–35 — we call `transactionService.postTransaction()` with all five fields unpacked from the request. Line 36 — return 201 Created with the response body.

**Lines 39–47 — `GET /transactions/{uuid}`.**
Line 42 — try block calls `transactionService.findById(uuid)`. Lines 43–44 — on success, wrap in 200 OK and return the response. Line 44–46 — catch `NoSuchElementException`, return 404. Because `findById` throws instead of returning Optional, we need this try-catch pattern. The alternative would be to return Optional from the service and use the `.map().orElse()` pattern — both work, this one makes the 'not found is exceptional' contract explicit in the service.

**Lines 49–57 — `GET /transactions/account/{accountId}`.**
Simple — call `findByAccountId`, stream the results through `TransactionResponse::from`, return 200 OK with the list. If the account has no transactions, this returns an empty list with 200, not a 404. That's the correct behavior.

**Lines 59–70 — `DELETE /transactions/{uuid}` — the most interesting endpoint.**
Line 62 — try block. Line 63 — call `transactionService.deleteTransaction(uuid)`, which has two potential throws inside it. Line 64 — if it completes without throwing, return 204 No Content. Line 65–66 — catch `NoSuchElementException`, return 404. This fires when the transaction doesn't exist or is already soft-deleted.
Line 67–69 — catch `IllegalStateException`, return `HttpStatus.CONFLICT` which is HTTP 409. This fires specifically when the transaction exists but `billed` is true. 409 is the right status code here — it's not a validation error, it's not unauthorized, it's a conflict between the request and the current state of the resource. The transaction is there, but its current state prevents the requested operation."

---

## STOP 17 — TransactionRow.java — the internal pattern
### File: src/main/java/io/github/lordship/transactions/internal/TransactionRow.java
### Lines: 11–49

"Open TransactionRow.java. Last file.

**Lines 11–20 — the record definition.**
Nine fields, one per column in the transaction table. Same as AccountRow, the `type` field at line 14 is a `String` — that's what JdbcClient maps from the VARCHAR column. The domain record holds `TransactionType` the enum.

**Lines 22–34 — `toTransaction()`.**
Line 26 — `TransactionType.valueOf(this.type)`. That's the String-to-enum conversion. Everything else maps straight across — amount stays BigDecimal, billingPeriod stays LocalDate, postedAt stays LocalDateTime, billed stays boolean.

**Lines 36–49 — the insert constructor.**
Five parameters at line 36 — `accountId`, `type` as `TransactionType` enum, `amount`, `description`, `billingPeriod`. Line 41 — `type.name()` converts the enum to its String name for storage. Everything the database generates — `uuid` at line 39, `postedAt` at line 45, and `deletedAt` at line 48 — is set to null. `billed` at line 46 is false. The database fills these in via `DEFAULT gen_random_uuid()`, `DEFAULT CURRENT_TIMESTAMP`, and `DEFAULT FALSE` respectively, and we get them back through `RETURNING *` in the INSERT statement."

---

## CLOSING — say this last

"So let me recap what I shipped in this PR.

Two new Flyway migrations — V11 adds three operational flags to the account table, V12 creates the full transaction table with the check constraint and two indexes.

On the accounts side, I tightened up the package structure by moving `AccountCreationRequest` into internal where it belongs. I removed `balance` from the update endpoint and built three independent layers of protection against it being changed — the request DTO, the controller's explicit use of `existing.balance()`, and the UPDATE SQL itself. I refactored `updateAccount` in the service to accept a full `Account` domain record instead of an eight-parameter list. And I added `getAccountByTenancyId` which the transaction module will need.

I wrote seven service-layer tests in `AccountServiceTest` — no MockMvc, direct service calls. The most important one proves the balance invariant: even if you pass a different balance all the way down to the service, the database ignores it.

The transactions module is a complete new package — eleven files total, three public and eight internal, following the exact same structure as accounts. Public enum, public domain record, public service, then controller, repository, row record, creation request, and response in internal. Modulith boundary test passes with it in.

What's coming next: wiring the `evictionInProgress` check into `postTransaction`, syncing `account.balance` back to the database after transactions are posted, and writing service-layer tests for the transactions module."

---

## QUICK REFERENCE — file locations and key line numbers

| File | Path | Key Lines |
|------|------|-----------|
| V11 migration | src/main/resources/db/migration/V11__account_flags.sql | 8–10 |
| V12 migration | src/main/resources/db/migration/V12__transactions.sql | 7–20 |
| Account.java | src/main/java/io/github/lordship/accounts/Account.java | 16–18 (new flags) |
| AccountRow.java | src/main/java/io/github/lordship/accounts/internal/AccountRow.java | 23–37 (toAccount), 39–53 (insert constructor) |
| AccountCreationRequest.java | src/main/java/io/github/lordship/accounts/internal/AccountCreationRequest.java | 1 (package changed) |
| AccountUpdateRequest.java | src/main/java/io/github/lordship/accounts/internal/AccountUpdateRequest.java | 6–22 (no balance) |
| AccountResponse.java | src/main/java/io/github/lordship/accounts/internal/AccountResponse.java | 17–19 (new flags), 21–33 (from factory) |
| AccountRepository.java | src/main/java/io/github/lordship/accounts/internal/AccountRepository.java | 21–24 (INSERT cols), 62–67 (UPDATE SET, no balance) |
| AccountService.java | src/main/java/io/github/lordship/accounts/AccountService.java | 31–33 (getByTenancyId), 43–58 (updateAccount refactor) |
| AccountController.java | src/main/java/io/github/lordship/accounts/internal/AccountController.java | 53 (fetch existing), 59 (existing.balance()), 62–64 (new flags from request) |
| AccountServiceTest.java | src/test/java/io/github/lordship/accounts/AccountServiceTest.java | 73–88 (defaults test), 139–164 (balance invariant test) |
| TransactionType.java | src/main/java/io/github/lordship/transactions/TransactionType.java | 4–13 (charges), 9–15 (credits/payments) |
| Transaction.java | src/main/java/io/github/lordship/transactions/Transaction.java | 8–18 (all fields) |
| TransactionService.java | src/main/java/io/github/lordship/transactions/TransactionService.java | 23–28 (post), 43–51 (delete + billed check), 53–55 (computeBalance) |
| TransactionRow.java | src/main/java/io/github/lordship/transactions/internal/TransactionRow.java | 22–34 (toTransaction), 36–49 (insert constructor) |
| TransactionRepository.java | src/main/java/io/github/lordship/transactions/internal/TransactionRepository.java | 57–71 (computeBalance SQL) |
| TransactionCreationRequest.java | src/main/java/io/github/lordship/transactions/internal/TransactionCreationRequest.java | 11–28 (all fields) |
| TransactionResponse.java | src/main/java/io/github/lordship/transactions/internal/TransactionResponse.java | 21–33 (from factory) |
| TransactionController.java | src/main/java/io/github/lordship/transactions/internal/TransactionController.java | 24 (accounts:edit), 59–70 (DELETE with 409) |
