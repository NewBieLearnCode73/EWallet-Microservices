package com.dinhchieu.ewallet.transaction_service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.dinhchieu.ewallet.common_library.dtos.BaseResponse;
import com.dinhchieu.ewallet.transaction_service.clients.dtos.response.WalletBalanceResponseDto;

@FeignClient(name = "wallet-service")
public interface WalletClient {
  @GetMapping("/balance")
  public ResponseEntity<BaseResponse<WalletBalanceResponseDto>> getBalance();
}
