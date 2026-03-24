package com.dinhchieu.ewallet.wallet_service.dtos.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletResultDto {
  private String status;
  private String transactionRefId;
  private Integer errorCode;
  private String errorMessage;
  private LocalDateTime timestamp;
}
