package com.dinhchieu.ewallet.profile_service.models.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinkedBankAccountsReponseDto {
  private String id;
  private String bankCode;
  private String accountNumber;
}
