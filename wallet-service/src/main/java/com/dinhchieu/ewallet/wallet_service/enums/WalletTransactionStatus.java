package com.dinhchieu.ewallet.wallet_service.enums;

import lombok.Getter;

@Getter
public enum WalletTransactionStatus {
  SUCCESS("Giao dịch thành công"),
  FAILURE("Giao dịch thất bại"),
  PENDING("Giao dịch đang chờ"),
  TIMEOUT("Giao dịch hết thời gian");

  private final String description;

  WalletTransactionStatus(String description) {
    this.description = description;
  }
}
