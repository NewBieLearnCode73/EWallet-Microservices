package com.dinhchieu.ewallet.bank_adapter_service.utils;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.dinhchieu.ewallet.bank_adapter_service.dtos.response.BankResult;
import com.dinhchieu.ewallet.bank_adapter_service.enums.BankCodeError;
import com.dinhchieu.ewallet.bank_adapter_service.enums.BankStatus;

@Component
public class BankResultUtils {
  public static BankResult createFailureResult(BankCodeError error) {
    return BankResult.builder()
        .status(BankStatus.FAILURE.name())
        .errorCode(error.getCode())
        .errorMessage(error.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
  }
}
