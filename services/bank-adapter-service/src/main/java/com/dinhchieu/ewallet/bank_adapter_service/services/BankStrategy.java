package com.dinhchieu.ewallet.bank_adapter_service.services;

import com.dinhchieu.ewallet.bank_adapter_service.dtos.response.BankResult;

public interface BankStrategy {

  /**
   * Get the bank code associated with this strategy.
   * Example: "VCB" for Vietcombank, "TCB" for Techcombank, etc.
   * 
   * @return the bank code as a String
   */
  String getBankCode();

  /**
   * Process a deposit transaction through the bank's API and return a BankResult
   * object
   * 
   * @param sagaId        the unique identifier for the transaction saga
   * @param amount        the amount to be processed
   * @param accountNumber the bank account number to which the payment will be
   *                      made
   * @return a BankResult object containing the outcome of the transaction
   */
  BankResult processDeposit(String sagaId, double amount, String accountNumber);

  /**
   * Process a withdrawal from the bank's API and return a BankResult object
   * 
   * @param sagaId        the unique identifier for the transaction saga
   * @param amount        the amount to be processed
   * @param accountNumber the bank account number to which the withdrawal will be
   *                      made
   * @return BankResult object with detailed transaction outcome information
   */
  BankResult processWithdrawal(String sagaId, double amount, String accountNumber);
}
