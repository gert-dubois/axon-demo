package com.gd.demo.axondemo.account.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record BlockAccountCommand(
        @TargetAggregateIdentifier String accountId
) {}
