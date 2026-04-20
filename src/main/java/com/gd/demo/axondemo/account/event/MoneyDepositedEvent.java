package com.gd.demo.axondemo.account.event;

import java.math.BigDecimal;
import java.util.UUID;

public record MoneyDepositedEvent(
		UUID transferId,
		String message,
		String accountId,
		String sourceAccountId,
        BigDecimal amount,
        BigDecimal previousBalance,
        BigDecimal newBalance
) {}
