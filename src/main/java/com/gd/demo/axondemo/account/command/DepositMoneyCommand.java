package com.gd.demo.axondemo.account.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.axonframework.modelling.command.TargetAggregateVersion;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositMoneyCommand(
		UUID transferId,
        @TargetAggregateIdentifier String accountId,
        BigDecimal amount,
		String message,
		String sourceAccountId
) {}
