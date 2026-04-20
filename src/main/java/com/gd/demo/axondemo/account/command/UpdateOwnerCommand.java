package com.gd.demo.axondemo.account.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.axonframework.modelling.command.TargetAggregateVersion;

public record UpdateOwnerCommand(
        @TargetAggregateIdentifier String accountId,
        String newOwner,
        @TargetAggregateVersion Long expectedVersion
) {}
