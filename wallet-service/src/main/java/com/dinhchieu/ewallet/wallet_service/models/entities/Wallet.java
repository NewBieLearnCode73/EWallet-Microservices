package com.dinhchieu.ewallet.wallet_service.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.dinhchieu.ewallet.wallet_service.enums.CurrencyType;
import com.dinhchieu.ewallet.wallet_service.enums.WalletStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "wallets")
public class Wallet {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "balance", nullable = false, precision = 19, scale = 2)
  @Builder.Default
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(name = "currency", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private CurrencyType currency = CurrencyType.VND;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private WalletStatus status = WalletStatus.INACTIVE;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false, updatable = true)
  @Builder.Default
  private LocalDateTime updatedAt = LocalDateTime.now();

  @Version
  private Long version;

  @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<WalletTransaction> transactions;
}
