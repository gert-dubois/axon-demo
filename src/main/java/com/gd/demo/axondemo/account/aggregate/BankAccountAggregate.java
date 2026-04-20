package com.gd.demo.axondemo.account.aggregate;

import com.gd.demo.axondemo.account.command.*;
import com.gd.demo.axondemo.account.event.*;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.eventsourcing.conflictresolution.ConflictResolver;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;

@Aggregate
public class BankAccountAggregate {

    @AggregateIdentifier
    private String accountId;
    private BigDecimal balance;
    private boolean blocked;
    private String owner;

    protected BankAccountAggregate() {}

    @CommandHandler
    public BankAccountAggregate(CreateAccountCommand cmd) {
        if (cmd.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        AggregateLifecycle.apply(new AccountCreatedEvent(cmd.accountId(), cmd.owner(), cmd.initialBalance()));
    }

    @CommandHandler
    public void handle(BlockAccountCommand cmd) {
        if (blocked) {
            throw new IllegalStateException("Account is already blocked");
        }
        AggregateLifecycle.apply(new AccountBlockedEvent(cmd.accountId()));
    }

    @CommandHandler
    public void handle(UpdateOwnerCommand cmd, ConflictResolver conflictResolver) {
        // Conflict: another owner update happened since our expected version
        conflictResolver.detectConflicts(events ->
                events.stream().anyMatch(e -> e.getPayload() instanceof OwnerUpdatedEvent));
        AggregateLifecycle.apply(new OwnerUpdatedEvent(cmd.accountId(), cmd.newOwner()));
    }

    @CommandHandler
    public void handle(DepositMoneyCommand cmd) {
        // LIVE CODE ↓ (scenario 1 — command handler)
//        if (blocked) {
//            throw new IllegalStateException("Account '" + accountId + "' is blocked and cannot receive money");
//        }
//        if (cmd.amount().compareTo(BigDecimal.ZERO) <= 0) {
//            throw new IllegalArgumentException("Deposit amount must be positive");
//        }
//        BigDecimal newBalance = balance.add(cmd.amount());
//        AggregateLifecycle.apply(new MoneyDepositedEvent(cmd.transferId(), cmd.message(), accountId, cmd.sourceAccountId(), cmd.amount(), balance, newBalance));
    }

    @CommandHandler
    public void handle(WithdrawMoneyCommand cmd, ConflictResolver conflictResolver) {
        if (blocked) {
            throw new IllegalStateException("Account '" + accountId + "' is blocked and cannot withdraw money");
        }
        if (cmd.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (balance.compareTo(cmd.amount()) < 0) {
            throw new IllegalStateException("Insufficient funds: balance is " + balance);
        }
        // LIVE CODE ↓ (scenario 5 — conflict resolution)
//        conflictResolver.detectConflicts(events ->
//                events.stream().anyMatch(e -> isMoneyEvent(e.getPayload())));
        BigDecimal newBalance = balance.subtract(cmd.amount());
        AggregateLifecycle.apply(new MoneyWithdrawnEvent(cmd.transferId(), cmd.message(), cmd.accountId(), null, cmd.amount(), balance, newBalance));
    }

    @CommandHandler
    public void handle(TransferMoneyCommand cmd, ConflictResolver conflictResolver) {
        if (blocked) {
            throw new IllegalStateException("Account '" + accountId + "' is blocked and cannot send money");
        }
        if (balance.compareTo(cmd.amount()) < 0) {
            throw new IllegalStateException("Insufficient funds for transfer: balance is " + balance);
        }
        conflictResolver.detectConflicts(events ->
                events.stream().anyMatch(e -> isMoneyEvent(e.getPayload())));
        BigDecimal newBalance = balance.subtract(cmd.amount());
        AggregateLifecycle.apply(new MoneyWithdrawnEvent(cmd.transferId(), cmd.message(), accountId, cmd.targetAccountId(), cmd.amount(), balance, newBalance));
    }

    @CommandHandler
    public void handle(RefundMoneyCommand cmd) {
        BigDecimal newBalance = balance.add(cmd.amount());
        AggregateLifecycle.apply(new MoneyRefundedEvent(cmd.transferId(), cmd.accountId(), cmd.amount(), balance, newBalance));
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent event) {
        this.accountId = event.accountId();
        this.owner = event.owner();
        this.balance = event.initialBalance();
        this.blocked = false;
    }

    @EventSourcingHandler
    public void on(AccountBlockedEvent event) {
        this.blocked = true;
    }

    @EventSourcingHandler
    public void on(OwnerUpdatedEvent event) {
        this.owner = event.newOwner();
    }

    @EventSourcingHandler
    public void on(MoneyWithdrawnEvent event) {
        this.balance = event.newBalance();
    }

    @EventSourcingHandler
    public void on(MoneyDepositedEvent event) {
        // LIVE CODE ↓ (scenario 2 — event sourcing handler)
//        this.balance = event.newBalance();
    }

    @EventSourcingHandler
    public void on(MoneyRefundedEvent event) {
        this.balance = event.newBalance();
    }

    private static boolean isMoneyEvent(Object payload) {
        return payload instanceof MoneyWithdrawnEvent
                || payload instanceof MoneyDepositedEvent
                || payload instanceof MoneyRefundedEvent;
    }
}
