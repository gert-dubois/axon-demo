package com.gd.demo.axondemo.account.projection;

import com.gd.demo.axondemo.account.event.*;
import com.gd.demo.axondemo.account.query.FindTransactionHistoryQuery;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.SequenceNumber;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@ProcessingGroup("transaction-history-projection")
public class TransactionHistoryProjection {

    private static final Logger log = LoggerFactory.getLogger(TransactionHistoryProjection.class);

    private final TransactionEntryRepository repository;

    public TransactionHistoryProjection(TransactionEntryRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void on(AccountCreatedEvent event, @SequenceNumber long seq, @Timestamp Instant timestamp) {
        TransactionEntry entry = TransactionEntry
                .builder(event.accountId(), "ACCOUNT_OPENED", seq, timestamp)
                .amount(event.initialBalance())
                .balanceAfter(event.initialBalance())
                .build();
        repository.save(entry);
        log.debug("[TransactionHistory] ACCOUNT_OPENED for '{}' — initial balance {}", event.accountId(), event.initialBalance());
    }

    /**
     * Withdrawal or transfer-out (when targetAccountId is present).
     * The type and counterpart are determined in a single event — no merging needed.
     */
    @EventHandler
    public void on(MoneyWithdrawnEvent event, @SequenceNumber long seq, @Timestamp Instant timestamp) {
        String transferId = event.transferId().toString();
        boolean isTransfer = event.targetAccountId() != null;
        String type = isTransfer ? "TRANSFER_OUT" : "WITHDRAWAL";

        TransactionEntry entry = repository.findByAccountIdAndTransferId(event.accountId(), transferId)
                .orElseGet(() -> TransactionEntry
                        .builder(event.accountId(), type, seq, timestamp)
                        .transferId(transferId)
                        .amount(event.amount())
                        .balanceBefore(event.previousBalance())
                        .balanceAfter(event.newBalance())
                        .message(nullIfBlank(event.message()))
                        .counterpartAccountId(event.targetAccountId())
                        .build());
        repository.save(entry);
        log.debug("[TransactionHistory] {} {} on '{}' — balance {} -> {}",
                type, event.amount(), event.accountId(), event.previousBalance(), event.newBalance());
    }

    /**
     * Deposit or transfer-in (when sourceAccountId is present).
     */
    @EventHandler
    public void on(MoneyDepositedEvent event, @SequenceNumber long seq, @Timestamp Instant timestamp) {
        String transferId = event.transferId().toString();
        boolean isTransfer = event.sourceAccountId() != null;
        String type = isTransfer ? "TRANSFER_IN" : "DEPOSIT";

        TransactionEntry entry = repository.findByAccountIdAndTransferId(event.accountId(), transferId)
                .orElseGet(() -> TransactionEntry
                        .builder(event.accountId(), type, seq, timestamp)
                        .transferId(transferId)
                        .amount(event.amount())
                        .balanceBefore(event.previousBalance())
                        .balanceAfter(event.newBalance())
                        .message(nullIfBlank(event.message()))
                        .counterpartAccountId(event.sourceAccountId())
                        .build());
        repository.save(entry);
        log.debug("[TransactionHistory] {} {} on '{}' — balance {} -> {}",
                type, event.amount(), event.accountId(), event.previousBalance(), event.newBalance());
    }

    /**
     * Saga compensation: finds the TRANSFER_OUT entry, blanks balance fields and marks it ABORTED.
     */
    @EventHandler
    public void on(MoneyRefundedEvent event, @SequenceNumber long seq, @Timestamp Instant timestamp) {
        String transferId = event.transferId().toString();
        TransactionEntry entry = repository.findByAccountIdAndTransferId(event.accountId(), transferId)
                .orElseGet(() -> TransactionEntry
                        .builder(event.accountId(), "ABORTED", seq, timestamp)
                        .transferId(transferId)
                        .amount(event.amount())
                        .build());
        entry.setType("ABORTED");
        entry.setBalanceBefore(null);
        entry.setBalanceAfter(null);
        repository.save(entry);
        log.debug("[TransactionHistory] ABORTED transfer {} on '{}' — refunded {}", transferId, event.accountId(), event.amount());
    }

    @QueryHandler
    public List<TransactionEntry> handle(FindTransactionHistoryQuery query) {
        return repository.findByAccountIdOrderBySequenceNumberAsc(query.accountId());
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
