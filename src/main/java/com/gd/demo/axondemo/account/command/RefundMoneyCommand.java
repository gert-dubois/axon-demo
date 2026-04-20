package com.gd.demo.axondemo.account.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundMoneyCommand(
		UUID transferId,
        @TargetAggregateIdentifier String accountId,
        BigDecimal amount
) {}
