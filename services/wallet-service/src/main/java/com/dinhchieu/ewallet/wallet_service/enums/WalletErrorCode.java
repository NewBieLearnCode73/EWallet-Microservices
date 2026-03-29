package com.dinhchieu.ewallet.wallet_service.enums;

import lombok.Getter;

@Getter
public enum WalletErrorCode {
  INSUFFICIENT_FUNDS(5001, "Số dư không đủ để thực hiện giao dịch!"),
  WALLET_NOT_FOUND(5002, "Ví không tồn tại!"),
  INVALID_TRANSACTION(5003, "Giao dịch không hợp lệ!"),
  TRANSACTION_TIMEOUT(5004, "Giao dịch đã hết thời gian xử lý!"),
  INTERNAL_SERVER_ERROR(5005, "Lỗi hệ thống! Vui lòng thử lại sau.");

  private final int code;
  private final String message;

  WalletErrorCode(int code, String message) {
    this.code = code;
    this.message = message;
  }
}
