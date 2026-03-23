package com.dinhchieu.ewallet.transaction_service.model.entity;

import java.math.BigDecimal;

import com.dinhchieu.ewallet.transaction_service.enums.TransactionStatus;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "transactions")
public class Transaction {
  @Id
  private String id;

  @Column(name = "type", nullable = false)
  @Enumerated(EnumType.STRING)
  private TransactionType type; // DEPOSIT, WITHDRAWAL, TRANSFER

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "source_wallet_id")
  private String sourceWalletId; // For DEPOSIT, this can be null

  @Column(name = "destination_wallet_id")
  private String destinationWalletId; // For WITHDRAWAL, this can be null

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private TransactionStatus status = TransactionStatus.PENDING; // PENDING, COMPLETED, FAILED

  @Column(name = "bank_code")
  private String bankCode; // Bank code (for withdraw/deposit from bank)

  @Column(name = "account_number")
  private String accountNumber; // Bank account number or wallet ID
}
