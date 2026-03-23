package com.dinhchieu.ewallet.bank_adapter_service.enums;

import lombok.Getter;

@Getter
public enum BankCodeError {
  BANK_CODE_SYSTEM_ERROR(4002, "Lỗi hệ thống ngân hàng!"),
  BANK_CODE_NOT_SUPPORTED(4003, "Mã ngân hàng không được hỗ trợ.");

  private final int code;
  private final String message;

  BankCodeError(int code, String message) {
    this.code = code;
    this.message = message;
  }
}
