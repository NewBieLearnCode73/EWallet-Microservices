package com.dinhchieu.ewallet.bank_adapter_service.enums;

import lombok.Getter;

@Getter
public enum BankStatus {
  SUCCESS("Giao dịch thành công"),
  FAILURE("Giao dịch thất bại"),
  PENDING("Giao dịch đang chờ"),
  TIMEOUT("Giao dịch hết thời gian");

  private final String description;

  BankStatus(String description) {
    this.description = description;
  }
}
