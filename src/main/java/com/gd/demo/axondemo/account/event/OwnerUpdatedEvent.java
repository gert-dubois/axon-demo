package com.gd.demo.axondemo.account.event;

public record OwnerUpdatedEvent(
        String accountId,
        String newOwner
) {}
