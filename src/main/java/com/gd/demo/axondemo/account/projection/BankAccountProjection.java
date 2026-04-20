package com.gd.demo.axondemo.account.projection;

import com.gd.demo.axondemo.account.event.*;
import com.gd.demo.axondemo.account.query.FindAccountQuery;
import com.gd.demo.axondemo.account.query.FindAllAccountsQuery;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.SequenceNumber;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ProcessingGroup("bank-account-projection")
public class BankAccountProjection {

    private static final Logger log = LoggerFactory.getLogger(BankAccountProjection.class);

    private final BankAccountViewRepository repository;

    public BankAccountProjection(BankAccountViewRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void on(AccountCreatedEvent event, DomainEventMessage<?> msg) {
        repository.save(new BankAccountView(event.accountId(), event.owner(), event.initialBalance(), msg.getSequenceNumber()));
        log.info("[BankAccountView] Created account '{}' for owner '{}' with balance {}", event.accountId(), event.owner(), event.initialBalance());
    }

    @EventHandler
    public void on(OwnerUpdatedEvent event, @SequenceNumber long version) {
        BankAccountView account = findOrThrow(event.accountId());
        account.setOwner(event.newOwner());
		account.setVersion(version);
        repository.save(account);
        log.info("[BankAccountView] Account '{}' owner updated to '{}' (v{})", event.accountId(), event.newOwner(), version);
    }

    @EventHandler
    public void on(AccountBlockedEvent event, @SequenceNumber long version) {
        BankAccountView account = findOrThrow(event.accountId());
        account.setBlocked(true);
		account.setVersion(version);
        repository.save(account);
        log.info("[BankAccountView] Account '{}' blocked (v{})", event.accountId(), version);
    }

    @EventHandler
    public void on(MoneyWithdrawnEvent event, @SequenceNumber long version) {
        BankAccountView account = findOrThrow(event.accountId());
        account.setBalance(event.newBalance());
		account.setVersion(version);
        repository.save(account);
        log.info("[BankAccountView] Account '{}' balance updated after withdrawal: {} → {} (v{})", event.accountId(), event.previousBalance(), event.newBalance(), version);
    }

    @EventHandler
    public void on(MoneyDepositedEvent event, @SequenceNumber long version) {
        // LIVE CODE ↓ (scenario 3 — event handler / projection)
//        BankAccountView account = findOrThrow(event.accountId());
//        account.setBalance(event.newBalance());
//        account.setVersion(version);
//        repository.save(account);
//        log.info("[BankAccountView] Account '{}' balance updated after deposit: {} → {} (v{})", event.accountId(), event.previousBalance(), event.newBalance(), version);
    }

    @EventHandler
    public void on(MoneyRefundedEvent event, @SequenceNumber long version) {
        BankAccountView account = findOrThrow(event.accountId());
        account.setBalance(event.newBalance());
		account.setVersion(version);
        repository.save(account);
        log.info("[BankAccountView] Account '{}' balance updated after refund: {} → {} (v{})", event.accountId(), event.previousBalance(), event.newBalance(), version);
    }

    @QueryHandler
    public BankAccountView handle(FindAccountQuery query) {
        return repository.findById(query.accountId()).orElse(null);
    }

    @QueryHandler
    public List<BankAccountView> handle(FindAllAccountsQuery query) {
        return repository.findAll();
    }

    private BankAccountView findOrThrow(String accountId) {
        return repository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Account not found in projection: " + accountId));
    }
}
