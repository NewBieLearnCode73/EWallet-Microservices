package com.dinhchieu.ewallet.wallet_service.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dinhchieu.ewallet.wallet_service.models.entities.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
}
