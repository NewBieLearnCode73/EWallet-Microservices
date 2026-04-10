package com.dinhchieu.ewallet.transaction_service.services;

import com.dinhchieu.ewallet.transaction_service.models.dtos.request.TransactionSearchRequestDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.DepositFromBankResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.InternalTransferResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.TransactionResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.TransactionSearchResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.WithdrawToBankResponseDto;

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
  DepositFromBankResponseDto processDepositFromBank(double amount, String bankCode, String accountNumber);

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
  WithdrawToBankResponseDto processWithdrawalFromBank(double amount, String bankCode, String accountNumber);

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
  InternalTransferResponseDto processInternalTransfer(double amount, String destinationWalletId);

  /**
   * Retrieve transaction details by transaction ID and return a
   * TransactionResponse
   * 
   * @param transactionId the ID of the transaction to be retrieved
   * @return a TransactionResponse object containing the details of the
   *         transaction
   */
  TransactionResponseDto getTransactionById(String transactionId);

  /**
   * Search for transactions by wallet ID with pagination and filtering
   * 
   * @param userId                      the ID of the authenticated user (for
   *                                    validation)
   * @param transactionSearchRequestDto the search criteria and pagination
   *                                    parameters
   * @return a TransactionSearchResponseDto containing the search results and
   *         pagination info
   */
  TransactionSearchResponseDto searchTransactions(String userId,
      TransactionSearchRequestDto transactionSearchRequestDto);

  /**
   * Admin search for all transactions with pagination and filtering
   * 
   * @param transactionSearchRequestDto the search criteria and pagination
   *                                    parameters
   * @return a TransactionSearchResponseDto containing the search results and
   *         pagination info
   */
  TransactionSearchResponseDto adminSearchAllTransactions(
      TransactionSearchRequestDto transactionSearchRequestDto);
}
