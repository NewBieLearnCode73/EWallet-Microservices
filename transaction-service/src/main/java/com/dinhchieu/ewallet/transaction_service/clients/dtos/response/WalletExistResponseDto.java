package com.dinhchieu.ewallet.transaction_service.clients.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WalletExistResponseDto {
  private boolean exist;
}
