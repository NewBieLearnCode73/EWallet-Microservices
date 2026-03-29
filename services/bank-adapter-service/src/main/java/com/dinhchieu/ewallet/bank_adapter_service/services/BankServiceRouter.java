package com.dinhchieu.ewallet.bank_adapter_service.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.dinhchieu.ewallet.bank_adapter_service.dtos.response.BankResult;
import com.dinhchieu.ewallet.bank_adapter_service.enums.BankCodeError;
import com.dinhchieu.ewallet.bank_adapter_service.utils.BankResultUtils;

@Service
public class BankServiceRouter {
  private final Map<String, BankStrategy> bankServiceMap = new HashMap<>();

  /**
   * Inject all BankStrategy implementations and populate the bankServiceMap for
   * routing based on bank code.
   */
  public BankServiceRouter(List<BankStrategy> bankStrategies) {
    for (BankStrategy strategy : bankStrategies) {
      bankServiceMap.put(strategy.getBankCode(), strategy);
    }
  }

  /**
   * Process a payment through the appropriate bank strategy based on the bank
   * code.
   * 
   * @param sagaId        the unique identifier for the transaction saga
   * @param bankCode      the code of the bank for which to process the payment
   * @param amount        the amount to be processed
   * @param accountNumber the bank account number to which the payment will be
   *                      made
   * @return a BankResult object containing the outcome of the transaction
   */
  public BankResult processDeposit(String sagaId, String bankCode, double amount, String accountNumber) {
    BankStrategy strategy = bankServiceMap.get(bankCode);
    if (strategy == null) {
      return BankResultUtils.createFailureResult(BankCodeError.BANK_CODE_NOT_SUPPORTED);
    }
    return strategy.processDeposit(sagaId, amount, accountNumber);
  }

  /**
   * Process a withdrawal through the appropriate bank strategy based on the bank
   * code and return a BankResult object that includes detailed information about
   * the
   * transaction outcome, such as status, transaction reference ID, error codes,
   * and messages.
   * 
   * @param sagaId        the unique identifier for the transaction saga
   * @param bankCode      the code of the bank for which to process the withdrawal
   * @param amount        the amount to be processed
   * @param accountNumber the bank account number to which the withdrawal will be
   *                      made
   * @return BankResult object with detailed transaction outcome information
   */
  public BankResult processWithdrawal(String sagaId, String bankCode, double amount, String accountNumber) {
    BankStrategy strategy = bankServiceMap.get(bankCode);
    if (strategy == null) {
      return BankResultUtils.createFailureResult(BankCodeError.BANK_CODE_NOT_SUPPORTED);
    }
    return strategy.processWithdrawal(sagaId, amount, accountNumber);
  }
}
