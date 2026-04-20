package com.gd.demo.axondemo.account.event;

import java.math.BigDecimal;
import java.util.UUID;

public record MoneyWithdrawnEvent(
		UUID transferId,
		String message,
		String accountId,
		String targetAccountId,
        BigDecimal amount,
        BigDecimal previousBalance,
        BigDecimal newBalance
) {}
