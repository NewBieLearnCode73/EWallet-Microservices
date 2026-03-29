package com.dinhchieu.ewallet.bank_adapter_service.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dinhchieu.ewallet.bank_adapter_service.dtos.response.BankResult;
import com.dinhchieu.ewallet.bank_adapter_service.enums.BankCodeError;
import com.dinhchieu.ewallet.bank_adapter_service.enums.BankStatus;
import com.dinhchieu.ewallet.bank_adapter_service.utils.BankResultUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TechBankService implements BankStrategy {

  @Override
  public String getBankCode() {
    return "TCB";
  }

  @Override
  public BankResult processDeposit(String sagaId, double amount, String accountNumber) {
    log.info("Processing deposit for TechBank: sagaId={}, amount={}, accountNumber={}", sagaId, amount, accountNumber);
    String tcbRef = "TCB-DEP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    if (accountNumber.startsWith("9999")) {
      return BankResult.builder().status(BankStatus.SUCCESS.name()).transactionRefId(tcbRef)
          .timestamp(LocalDateTime.now()).build();
    }
    return BankResultUtils.createFailureResult(BankCodeError.BANK_CODE_SYSTEM_ERROR);
  }

  @Override
  public BankResult processWithdrawal(String sagaId, double amount, String accountNumber) {
    log.info("Processing withdrawal for TechBank: sagaId={}, amount={}, accountNumber={}", sagaId, amount,
        accountNumber);
    String tcbRef = "TCB-WDL" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    if (accountNumber.startsWith("9999")) {
      return BankResult.builder().status(BankStatus.SUCCESS.name()).transactionRefId(tcbRef)
          .timestamp(LocalDateTime.now()).build();
    }

    return BankResultUtils.createFailureResult(BankCodeError.BANK_CODE_SYSTEM_ERROR);
  }

}
