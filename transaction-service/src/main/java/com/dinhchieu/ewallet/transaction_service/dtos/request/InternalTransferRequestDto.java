package com.dinhchieu.ewallet.transaction_service.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InternalTransferRequestDto {
  @NotNull(message = "Số tiền không được để trống")
  @Positive(message = "Số tiền phải lớn hơn 0")
  private double amount;

  @NotBlank(message = "Mã ngân hàng không được để trống")
  private String destinationWalletId;
}
