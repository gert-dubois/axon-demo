package com.gd.demo.axondemo.account.projection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionEntryRepository extends JpaRepository<TransactionEntry, Long> {

    List<TransactionEntry> findByAccountIdOrderBySequenceNumberAsc(String accountId);

    Optional<TransactionEntry> findByAccountIdAndTransferId(String accountId, String transferId);
}
