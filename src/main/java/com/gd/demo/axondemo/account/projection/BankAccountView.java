package com.gd.demo.axondemo.account.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "bank_account")
public class BankAccountView {

    @Id
    @Column(name = "account_id")
    private String accountId;

    private String owner;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance;

    private boolean blocked;

    private long version;

    protected BankAccountView() {}

    public BankAccountView(String accountId, String owner, BigDecimal balance, long version) {
        this.accountId = accountId;
        this.owner = owner;
        this.balance = balance;
        this.blocked = false;
        this.version = version;
    }

    public String getAccountId() { return accountId; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }
    public boolean isBlocked() { return blocked; }
    public long getVersion() { return version; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public void setVersion(long version) { this.version = version; }
}
