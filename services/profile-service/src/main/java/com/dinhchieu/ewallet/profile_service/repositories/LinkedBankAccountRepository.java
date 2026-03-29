package com.dinhchieu.ewallet.profile_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dinhchieu.ewallet.profile_service.models.entities.LinkedBankAccount;

@Repository
public interface LinkedBankAccountRepository extends JpaRepository<LinkedBankAccount, String> {
  boolean existsByBankCodeAndAccountNumber(String bankCode, String accountNumber);
}
