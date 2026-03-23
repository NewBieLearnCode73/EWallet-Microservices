package com.dinhchieu.ewallet.transaction_service.service;

import com.dinhchieu.ewallet.transaction_service.dtos.response.DepositFromBankResponse;
import com.dinhchieu.ewallet.transaction_service.dtos.response.WithdrawToBankResponse;

public interface TransactionService {
  /**
   * Process a deposit transaction from the bank and return a
   * DepositFromBankResponse object
   * 
   * @param amount        the amount to be processed
   * @param bankCode      the code of the bank from which the deposit will be made
   * @param accountNumber the bank account number from which the deposit will be
   *                      made
   * @return a DepositFromBankResponse object containing the outcome of the
   *         transaction
   */
  DepositFromBankResponse processDepositFromBank(double amount, String bankCode, String accountNumber);

  /**
   * Process a withdrawal transaction from the bank and return a
   * WithdrawToBankResponse object
   *
   * @param amount        the amount to be processed
   * @param bankCode      the code of the bank from which the withdrawal will be
   *                      made
   * @param accountNumber the bank account number from which the withdrawal will
   *                      be
   *                      made
   * @return a WithdrawToBankResponse object containing the outcome of the
   *         transaction
   */
  WithdrawToBankResponse processWithdrawalFromBank(double amount, String bankCode, String accountNumber);
}
