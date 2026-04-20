package com.gd.demo.axondemo.account.projection;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_history", indexes = {
        @Index(name = "idx_tx_account_id", columnList = "account_id")
})
public class TransactionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String type;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    private String message;

    @Column(name = "transfer_id")
    private String transferId;

    @Column(name = "counterpart_account_id")
    private String counterpartAccountId;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "occurred_on", nullable = false)
    private Instant occurredOn;

    protected TransactionEntry() {}

    private TransactionEntry(Builder builder) {
        this.accountId = builder.accountId;
        this.type = builder.type;
        this.amount = builder.amount;
        this.balanceBefore = builder.balanceBefore;
        this.balanceAfter = builder.balanceAfter;
        this.message = builder.message;
        this.transferId = builder.transferId;
        this.counterpartAccountId = builder.counterpartAccountId;
        this.sequenceNumber = builder.sequenceNumber;
        this.occurredOn = builder.occurredOn;
    }

    public Long getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getMessage() { return message; }
    public String getTransferId() { return transferId; }
    public String getCounterpartAccountId() { return counterpartAccountId; }
    public long getSequenceNumber() { return sequenceNumber; }
    public Instant getOccurredOn() { return occurredOn; }

    public void setType(String type) { this.type = type; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public void setMessage(String message) { this.message = message; }
    public void setCounterpartAccountId(String counterpartAccountId) { this.counterpartAccountId = counterpartAccountId; }

    public static Builder builder(String accountId, String type, long sequenceNumber, Instant occurredOn) {
        return new Builder(accountId, type, sequenceNumber, occurredOn);
    }

    public static class Builder {
        private final String accountId;
        private final String type;
        private final long sequenceNumber;
        private final Instant occurredOn;
        private BigDecimal amount;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;
        private String message;
        private String transferId;
        private String counterpartAccountId;

        private Builder(String accountId, String type, long sequenceNumber, Instant occurredOn) {
            this.accountId = accountId;
            this.type = type;
            this.sequenceNumber = sequenceNumber;
            this.occurredOn = occurredOn;
        }

        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder balanceBefore(BigDecimal v) { this.balanceBefore = v; return this; }
        public Builder balanceAfter(BigDecimal v) { this.balanceAfter = v; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder transferId(String transferId) { this.transferId = transferId; return this; }
        public Builder counterpartAccountId(String id) { this.counterpartAccountId = id; return this; }

        public TransactionEntry build() { return new TransactionEntry(this); }
    }
}
