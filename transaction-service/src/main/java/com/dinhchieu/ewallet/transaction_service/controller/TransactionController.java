package com.dinhchieu.ewallet.transaction_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.dinhchieu.ewallet.common_library.dtos.BaseResponse;
import com.dinhchieu.ewallet.transaction_service.dtos.request.InternalTransferRequestDto;
import com.dinhchieu.ewallet.transaction_service.dtos.request.TransactionDepositRequestDto;
import com.dinhchieu.ewallet.transaction_service.dtos.request.TransactionWithdrawRequestDto;
import com.dinhchieu.ewallet.transaction_service.service.TransactionService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@AllArgsConstructor
public class TransactionController {
  private final TransactionService transactionService;

  @PostMapping("/deposit")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> deposit(@Valid @RequestBody TransactionDepositRequestDto request) {
    var result = transactionService.processDepositFromBank(request.getAmount(), request.getBankCode(),
        request.getAccountNumber());
    return ResponseEntity.ok(BaseResponse.builder().message("Nạp tiền thành công").data(result).build());
  }

  @PostMapping("/withdraw")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> withdraw(@Valid @RequestBody TransactionWithdrawRequestDto request) {
    var result = transactionService.processWithdrawalFromBank(request.getAmount(), request.getBankCode(),
        request.getAccountNumber());
    return ResponseEntity.ok(BaseResponse.builder().message("Rút tiền thành công").data(result).build());
  }

  @PostMapping("/transfer")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> transfer(@Valid @RequestBody InternalTransferRequestDto request) {
    var result = transactionService.processInternalTransfer(request.getAmount(), request.getDestinationWalletId());
    return ResponseEntity.ok(BaseResponse.builder().message("Chuyển tiền thành công").data(result).build());
  }
}