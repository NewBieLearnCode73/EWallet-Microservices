package com.dinhchieu.ewallet.transaction_service.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.dinhchieu.ewallet.common_library.dtos.BaseResponse;
import com.dinhchieu.ewallet.common_library.utils.SecurityUtils;
import com.dinhchieu.ewallet.transaction_service.models.dtos.request.InternalTransferRequestDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.request.TransactionDepositRequestDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.request.TransactionSearchRequestDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.request.TransactionWithdrawRequestDto;
import com.dinhchieu.ewallet.transaction_service.services.TransactionService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@AllArgsConstructor
public class TransactionController {
  private final TransactionService transactionService;

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> getTransactionById(@PathVariable("id") String id) {
    return ResponseEntity.ok(BaseResponse.builder().message("Lấy thông tin giao dịch thành công")
        .data(transactionService.getTransactionById(id)).build());
  }

  @GetMapping("/search")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BaseResponse<Object>> searchTransactions(
      @Valid @RequestBody TransactionSearchRequestDto searchRequest) {
    var userId = SecurityUtils.getAuthenticatedUserId().toString();
    var result = transactionService.searchTransactions(userId, searchRequest);
    return ResponseEntity.ok(BaseResponse.builder().message("Tìm kiếm giao dịch thành công").data(result).build());
  }

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

  @GetMapping("/admin/search")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<BaseResponse<Object>> adminSearchAllTransactions(
      @Valid @RequestBody TransactionSearchRequestDto searchRequest) {
    var result = transactionService.adminSearchAllTransactions(searchRequest);
    return ResponseEntity
        .ok(BaseResponse.builder().message("Admin tìm kiếm giao dịch thành công").data(result).build());
  }
}