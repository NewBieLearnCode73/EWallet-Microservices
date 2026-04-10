package com.dinhchieu.ewallet.transaction_service.models.dtos.response;

import java.util.List;

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
public class TransactionSearchResponseDto {

  private List<TransactionResponseDto> transactions;

  private int currentPage;

  private int pageSize;

  private long totalElements;

  private int totalPages;

  private boolean isFirst;

  private boolean isLast;

  private boolean hasNext;

  private boolean hasPrevious;
}
