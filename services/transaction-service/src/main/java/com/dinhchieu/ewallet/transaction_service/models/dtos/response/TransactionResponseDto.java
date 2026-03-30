package com.dinhchieu.ewallet.transaction_service.models.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.dinhchieu.ewallet.transaction_service.enums.TransactionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponseDto {
  private String transactionId;
  private BigDecimal amount;
  private String type;

  private String sourceWalletId;
  private String sourceUserName;

  private String destinationWalletId;
  private String destinationUserName;

  private String description;
  private TransactionStatus status;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}