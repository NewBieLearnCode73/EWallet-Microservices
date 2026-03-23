package com.dinhchieu.ewallet.transaction_service.enums;

import lombok.Getter;

@Getter
public enum EventStatus {
  SUCCESS("Giao dịch thành công"),
  FAILURE("Giao dịch thất bại"),
  PENDING("Giao dịch đang chờ"),
  TIMEOUT("Giao dịch hết thời gian");

  private final String description;

  EventStatus(String description) {
    this.description = description;
  }
}
