package com.gd.demo.axondemo.account.projection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankAccountViewRepository extends JpaRepository<BankAccountView, String> {}
