package com.dinhchieu.ewallet.wallet_service.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dinhchieu.ewallet.wallet_service.models.entities.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
  @Query(value = "SELECT * FROM wallets WHERE id = :id FOR UPDATE", nativeQuery = true)
  Optional<Wallet> findByIdWithLock(@Param("id") UUID id);
}
