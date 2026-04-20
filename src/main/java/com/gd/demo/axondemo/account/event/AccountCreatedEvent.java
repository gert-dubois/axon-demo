package com.gd.demo.axondemo.account.event;

import java.math.BigDecimal;

public record AccountCreatedEvent(
        String accountId,
        String owner,
        BigDecimal initialBalance
) {}
