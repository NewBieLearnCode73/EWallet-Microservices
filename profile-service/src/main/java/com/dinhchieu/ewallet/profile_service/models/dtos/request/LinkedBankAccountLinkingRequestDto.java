package com.dinhchieu.ewallet.profile_service.models.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkedBankAccountLinkingRequestDto {
  @NotBlank(message = "Mã ngân hàng không được để trống")
  private String bankCode;

  @NotBlank(message = "Số tài khoản không được để trống")
  @Size(min = 10, max = 20, message = "Số tài khoản phải có độ dài từ 10 đến 20 ký tự")
  private String accountNumber;
}
