package com.dinhchieu.ewallet.transaction_service.models.dtos.response;

import com.dinhchieu.ewallet.transaction_service.enums.TransactionStatus;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InternalTransferResponseDto {
  private String transactionId;
  private double amount;
  private TransactionType type;
  private String sourceWalletId;
  private String destinationWalletId;
  @Builder.Default
  @Enumerated(EnumType.STRING)
  private TransactionStatus status = TransactionStatus.PENDING;
}
