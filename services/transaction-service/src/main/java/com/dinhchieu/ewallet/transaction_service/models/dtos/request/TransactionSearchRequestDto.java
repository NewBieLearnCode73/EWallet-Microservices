package com.dinhchieu.ewallet.transaction_service.models.dtos.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionSearchRequestDto {

  private String walletId;

  private String transactionType;

  private String status;

  @Min(value = 0, message = "Page number must be greater than or equal to 0")
  private Integer page;

  @Min(value = 1, message = "Page size must be greater than 0")
  private Integer pageSize;

  private String sortBy;

  private String sortOrder;
}
