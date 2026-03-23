package com.dinhchieu.ewallet.wallet_service.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinhchieu.ewallet.common_library.dtos.BaseResponse;
import com.dinhchieu.ewallet.wallet_service.dtos.response.WalletBalanceResponse;
import com.dinhchieu.ewallet.wallet_service.services.WalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WalletController {
  private final WalletService walletService;

  @GetMapping("/balance")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> getBalance() {
    WalletBalanceResponse balance = walletService.getBalance();
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Lấy số dư ví thành công")
        .data(balance)
        .build());
  }

  @PatchMapping("/activate")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> activateWallet() {
    walletService.activeWallet();
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Kích hoạt ví thành công")
        .build());
  }
}
