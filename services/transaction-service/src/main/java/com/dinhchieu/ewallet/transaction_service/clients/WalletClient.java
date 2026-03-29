package com.dinhchieu.ewallet.transaction_service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.dinhchieu.ewallet.common_library.dtos.BaseResponse;
import com.dinhchieu.ewallet.transaction_service.clients.dtos.response.WalletBalanceResponseDto;
import com.dinhchieu.ewallet.transaction_service.clients.dtos.response.WalletExistResponseDto;

@FeignClient(name = "wallet-service")
public interface WalletClient {
  @GetMapping("/balance")
  public ResponseEntity<BaseResponse<WalletBalanceResponseDto>> getBalance();

  @GetMapping("/exists/{userId}")
  public ResponseEntity<BaseResponse<WalletExistResponseDto>> isWalletExists(@PathVariable String userId);
}
