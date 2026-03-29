package com.dinhchieu.ewallet.transaction_service.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dinhchieu.ewallet.transaction_service.models.entities.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
}
