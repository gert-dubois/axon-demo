package com.gd.demo.axondemo.account.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

public record CreateAccountCommand(
        @TargetAggregateIdentifier String accountId,
        String owner,
        BigDecimal initialBalance
) {}
