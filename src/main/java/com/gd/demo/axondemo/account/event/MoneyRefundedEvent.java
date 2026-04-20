package com.gd.demo.axondemo.account.event;

import java.math.BigDecimal;
import java.util.UUID;

public record MoneyRefundedEvent(
		UUID transferId,
		String accountId,
        BigDecimal amount,
        BigDecimal previousBalance,
        BigDecimal newBalance
) {}
