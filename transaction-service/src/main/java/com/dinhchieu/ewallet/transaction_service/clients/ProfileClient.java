package com.dinhchieu.ewallet.transaction_service.clients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.dinhchieu.ewallet.common_library.dtos.BaseResponse;
import com.dinhchieu.ewallet.transaction_service.clients.dtos.response.LinkedBankAccountsReponseDto;

@FeignClient(name = "profile-service")
public interface ProfileClient {
  @GetMapping("/me/linked-bank-accounts")
  public ResponseEntity<BaseResponse<List<LinkedBankAccountsReponseDto>>> getMyLinkedBankAccounts();
}
