package com.dinhchieu.ewallet.wallet_service.models.dtos.response;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WalletBalanceResponseDto implements Serializable {
  private String walletId;
  private BigDecimal balance;
}
