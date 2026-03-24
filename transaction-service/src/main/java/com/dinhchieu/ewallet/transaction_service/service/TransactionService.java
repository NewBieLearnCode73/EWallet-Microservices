package com.dinhchieu.ewallet.transaction_service.service;

import com.dinhchieu.ewallet.transaction_service.dtos.response.DepositFromBankResponse;
import com.dinhchieu.ewallet.transaction_service.dtos.response.InternalTransferResponse;
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

  /**
   * Process an internal transfer transaction between two wallets and return an
   * InternalTransferResponse object
   * 
   * @param amount              the amount to be processed
   * @param sourceWalletId      the ID of the source wallet from which the
   *                            transfer
   *                            will be made
   * @param destinationWalletId the ID of the destination wallet to which the
   *                            transfer
   *                            will be made
   * @return an InternalTransferResponse object containing the outcome of the
   * 
   */
  InternalTransferResponse processInternalTransfer(double amount, String destinationWalletId);
}
