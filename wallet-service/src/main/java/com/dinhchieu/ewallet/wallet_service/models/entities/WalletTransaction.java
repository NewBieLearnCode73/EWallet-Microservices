package com.dinhchieu.ewallet.wallet_service.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.dinhchieu.ewallet.wallet_service.enums.TransactionStatus;
import com.dinhchieu.ewallet.wallet_service.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "transaction_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private TransactionType transactionType;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private TransactionStatus status;

  @Column(name = "saga_id")
  private UUID sagaId;

  @Column(name = "destination_wallet_id")
  private UUID destinationWalletId;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false, updatable = true)
  @Builder.Default
  private LocalDateTime updatedAt = LocalDateTime.now();

  // Avoid N+1 problem
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Wallet wallet;
}
