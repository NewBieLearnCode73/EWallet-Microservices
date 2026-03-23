package com.dinhchieu.ewallet.transaction_service.clients.dtos.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WalletBalanceResponseDto {
  private String walletId;
  private BigDecimal balance;
}
