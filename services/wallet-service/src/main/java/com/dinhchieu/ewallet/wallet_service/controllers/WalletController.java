package com.dinhchieu.ewallet.wallet_service.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.dinhchieu.ewallet.common_library.dtos.BaseResponse;
import com.dinhchieu.ewallet.common_library.utils.SecurityUtils;
import com.dinhchieu.ewallet.wallet_service.models.dtos.response.WalletBalanceResponseDto;
import com.dinhchieu.ewallet.wallet_service.services.WalletService;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WalletController {
  private final WalletService walletService;

  @GetMapping("/exists/{userId}")
  @PermitAll
  public ResponseEntity<BaseResponse<Object>> isWalletExists(@PathVariable String userId) {

    return ResponseEntity.ok(BaseResponse.builder()
        .message("Kiểm tra tồn tại ví thành công")
        .data(walletService.isWalletExist(UUID.fromString(userId)))
        .build());
  }

  @GetMapping("/balance")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> getBalance() {
    UUID userId = SecurityUtils.getAuthenticatedUserId();

    WalletBalanceResponseDto balance = walletService.getBalance(userId);
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Lấy số dư ví thành công")
        .data(balance)
        .build());
  }

  @PatchMapping("/activate")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> activateWallet() {
    UUID userId = SecurityUtils.getAuthenticatedUserId();

    walletService.activeWallet(userId);
    return ResponseEntity.ok(BaseResponse.builder()
        .message("Kích hoạt ví thành công")
        .build());
  }
}
