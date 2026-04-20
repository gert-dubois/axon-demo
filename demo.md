# Axon Framework Live Coding Demo

> **Setup:** Start the app (`./mvnw spring-boot:run`), open a second terminal for the CLI.  
> Use `create acc1 Alice 1000` to seed data between scenarios.

---

## Scenario 1 — Command Handlers (~10 min)

**Concept:** Commands express *intent*. A command handler validates that intent and, if valid, applies one or more events to the aggregate.

**File:** `BankAccountAggregate.java` → `handle(DepositMoneyCommand)`

**What to uncomment:**
```java
if (blocked) { throw ... }
if (cmd.amount() <= 0) { throw ... }
BigDecimal newBalance = balance.add(cmd.amount());
AggregateLifecycle.apply(new MoneyDepositedEvent(...));
```

**Demo steps:**
1. Show the empty method body — run `deposit acc1 100` and note nothing happens
2. Uncomment validation + `AggregateLifecycle.apply(...)`
3. Run `deposit acc1 100` → balance updates
4. Try `deposit acc1 -50` → validation fires

**Key points:**
- `@CommandHandler` on constructor creates the aggregate
- `AggregateLifecycle.apply()` persists the event — it does **not** mutate state directly
- State mutation happens in the `@EventSourcingHandler` (next scenario)

---

## Scenario 2 — Event Sourcing Handlers (~10 min)

**Concept:** The aggregate rebuilds its state by replaying events. `@EventSourcingHandler` is the only place state fields are mutated.

**File:** `BankAccountAggregate.java` → `on(MoneyDepositedEvent)` (EventSourcingHandler)

**What to uncomment:**
```java
this.balance = event.newBalance();
```

**Demo steps:**
1. After scenario 1 — deposits work but `balance` is stale (always shows old value)
2. Uncomment `this.balance = event.newBalance()`
3. Restart — Axon replays all stored events and reconstructs balance correctly
4. Show other ESH examples (`AccountCreatedEvent`, `MoneyWithdrawnEvent`) for comparison

**Key points:**
- ESH must be **pure** — no side effects, no I/O
- On restart Axon replays all events in order to rebuild aggregate state
- This is the "sourcing" in Event Sourcing

---

## Scenario 3 — Projections & Event Handlers (~10 min)

**Concept:** Projections are the read side. They listen to events and build a query-optimised view — completely separate from the aggregate.

**File:** `BankAccountProjection.java` → `on(MoneyDepositedEvent)` (EventHandler)

**What to uncomment:**
```java
BankAccountView account = findOrThrow(event.accountId());
account.setBalance(event.newBalance());
account.setVersion(version);
repository.save(account);
log.info(...);
```

**Demo steps:**
1. Run `deposit acc1 100` — command succeeds but `balance acc1` shows stale balance
2. Uncomment the event handler body
3. Run `deposit acc1 100` again → `balance acc1` now reflects new balance
4. Point out `@QueryHandler` and how the CLI queries the projection

**Key points:**
- `@EventHandler` ≠ `@EventSourcingHandler` — different lifecycle, different purpose
- Projections can be rebuilt by replaying events (tracking processors)
- Multiple projections can consume the same event independently

---

## Scenario 4 — Subscribing vs Tracking Processors (~10 min)

**Concept:** Axon supports two processing modes. *Tracking* processors maintain their own position in the event stream (can replay, survive restarts). *Subscribing* processors consume events in-process, synchronously with the publisher.

**Files:**
- `TransactionHistoryProjection.java` — has `@ProcessingGroup("transaction-history-projection")`
- `application.yml` — `mode: subscribing` under that group

**What to show:**
1. Comment out `mode: subscribing` in yml (or change to `mode: tracking`)  
2. Restart — processor now uses tracking mode; show it catching up in logs
3. Uncomment `mode: subscribing` and restart — now runs synchronously inline

**Key points:**
- **Tracking** (default): independent position, survives restarts, supports replay, runs in background thread
- **Subscribing**: no stored position, runs in the caller's thread, simpler but can't replay
- `@ProcessingGroup` groups handlers under one named processor so you can configure them together
- For the transaction history projection, subscribing makes sense — it's non-critical and we want low latency

---

## Scenario 5 — Conflict Resolution (~10 min)

**Concept:** Two users read the same account version and both try to withdraw. Without conflict detection, both succeed — a concurrency bug. Axon's `ConflictResolver` lets you detect and reject conflicting concurrent commands.

**File:** `BankAccountAggregate.java` → `handle(WithdrawMoneyCommand)`

**What to uncomment:**
```java
conflictResolver.detectConflicts(events ->
        events.stream().anyMatch(e -> isMoneyEvent(e.getPayload())));
```

**Demo steps:**
1. Seed: `create acc1 Alice 1000`
2. Open two CLI windows, both run `balance acc1` — both see version 0
3. Without conflict detection: `withdraw acc1 800` from both → both succeed, balance goes negative
4. Uncomment `conflictResolver.detectConflicts(...)`
5. Repeat — second withdraw now throws a conflict exception

**Key points:**
- `@TargetAggregateVersion` on the command carries the client's expected version
- `ConflictResolver` compares it against events applied since that version
- This is **optimistic locking** — no DB locks held, conflicts detected on commit
- You define what counts as a conflict (e.g. another money event since our version)

---

## Scenario 6 — Sagas (~10 min)

**Concept:** A Saga manages a long-running business transaction that spans multiple aggregates. It listens to events and sends commands, including **compensating commands** when something goes wrong.

**File:** `MoneyTransferSaga.java` → `on(MoneyWithdrawnEvent)` — the `commandGateway.send(...)` block

**What to uncomment:**
```java
commandGateway.send(new DepositMoneyCommand(
        event.transferId(), event.targetAccountId(), event.amount(),
        event.message(), event.accountId()
)).exceptionally(ex -> {
    System.out.println("[SAGA] Credit failed ...");
    commandGateway.send(new RefundMoneyCommand(...));
    return null;
});
```

**Demo steps:**
1. `create acc1 Alice 1000`, `create acc2 Bob 500`
2. `transfer acc1 acc2 200` — without the saga body, money disappears (withdrawn, never deposited)
3. Uncomment the saga body
4. `transfer acc1 acc2 200` — succeeds, both balances update, `[SAGA]` log appears
5. `block acc2`, then `transfer acc1 acc2 200` — deposit fails, refund fires automatically

**Key points:**
- `@StartSaga` + `@SagaEventHandler` ties saga instances to a `transferId` association
- `SagaLifecycle.end()` cleans up — called immediately for plain withdrawals (no target)
- `@EndSaga` marks the terminal events (success = deposit, failure = refund)
- The saga holds no business state in the aggregate — it's purely an orchestrator
