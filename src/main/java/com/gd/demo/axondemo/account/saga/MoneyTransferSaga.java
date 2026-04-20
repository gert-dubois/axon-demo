package com.gd.demo.axondemo.account.saga;

import com.gd.demo.axondemo.account.command.DepositMoneyCommand;
import com.gd.demo.axondemo.account.command.RefundMoneyCommand;
import com.gd.demo.axondemo.account.event.MoneyDepositedEvent;
import com.gd.demo.axondemo.account.event.MoneyRefundedEvent;
import com.gd.demo.axondemo.account.event.MoneyWithdrawnEvent;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

@Saga
public class MoneyTransferSaga {

    @Autowired
    private transient CommandGateway commandGateway;

    /**
     * Every MoneyWithdrawnEvent starts a saga instance.
     * If there is no targetAccountId this is a plain withdrawal — end the saga immediately.
     */
    @StartSaga
    @SagaEventHandler(associationProperty = "transferId")
    public void on(MoneyWithdrawnEvent event) {
        if (event.targetAccountId() == null) {
            SagaLifecycle.end();
            return;
        }

        commandGateway.send(new DepositMoneyCommand(
                event.transferId(),
                event.targetAccountId(),
                event.amount(),
                event.message(),
                event.accountId()  // source account
        )).exceptionally(ex -> {
            System.out.println("[SAGA] Credit failed (" + ex.getMessage() + ") — compensating transfer " + event.transferId());
            commandGateway.send(new RefundMoneyCommand(event.transferId(), event.accountId(), event.amount()));
            return null;
        });
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "transferId")
    public void on(MoneyDepositedEvent event) {
        System.out.println("[SAGA] Transfer " + event.transferId() + " completed successfully.");
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "transferId")
    public void on(MoneyRefundedEvent event) {
        System.out.println("[SAGA] Compensation complete — refunded " + event.amount() + " to account '" + event.accountId() + "'.");
    }
}
