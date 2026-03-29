package com.dinhchieu.ewallet.profile_service.models.dtos.response;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinkedBankAccountsReponseDto implements Serializable {
  private String id;
  private String bankCode;
  private String accountNumber;
}
