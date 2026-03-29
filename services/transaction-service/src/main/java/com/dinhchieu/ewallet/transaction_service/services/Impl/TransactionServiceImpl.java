package com.dinhchieu.ewallet.transaction_service.services.Impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dinhchieu.ewallet.avro.BankAction;
import com.dinhchieu.ewallet.avro.BankCommand;
import com.dinhchieu.ewallet.avro.WalletAction;
import com.dinhchieu.ewallet.avro.WalletCommand;
import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.common_library.utils.SecurityUtils;
import com.dinhchieu.ewallet.transaction_service.clients.ProfileClient;
import com.dinhchieu.ewallet.transaction_service.clients.WalletClient;
import com.dinhchieu.ewallet.transaction_service.clients.dtos.response.LinkedBankAccountsReponseDto;
import com.dinhchieu.ewallet.transaction_service.clients.dtos.response.WalletBalanceResponseDto;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionStatus;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionType;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionWalletCommandType;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.DepositFromBankResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.InternalTransferResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.WithdrawToBankResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.entities.Transaction;
import com.dinhchieu.ewallet.transaction_service.repositories.TransactionRepository;
import com.dinhchieu.ewallet.transaction_service.sagas.outbox.OutboxMessage;
import com.dinhchieu.ewallet.transaction_service.sagas.outbox.OutboxMessageRepository;
import com.dinhchieu.ewallet.transaction_service.services.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

  private final WalletClient walletClient;
  private final ProfileClient profileClient;
  private final ObjectMapper objectMapper;
  private final OutboxMessageRepository outboxMessageRepository;
  private final TransactionRepository transactionRepository;

  // Deposit : Bank -> Wallet : Nạp tiền từ ngân hàng vào ví
  // Trừ tiền ngân hàng trước
  @Override
  public DepositFromBankResponseDto processDepositFromBank(double amount, String bankCode, String accountNumber) {

    try {
      UUID sagaId = UUID.randomUUID();
      UUID userId = SecurityUtils.getAuthenticatedUserId();

      var linkedAccountsResponse = profileClient.getMyLinkedBankAccounts();
      if (linkedAccountsResponse == null || linkedAccountsResponse.getBody() == null) {
        log.error("Failed to fetch linked bank accounts from profile service");
        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
      }

      List<LinkedBankAccountsReponseDto> linkedBankAccounts = linkedAccountsResponse.getBody().getData();

      if (linkedBankAccounts == null || linkedBankAccounts.isEmpty()) {
        log.error("User has no linked bank accounts");
        throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
      }

      boolean isLinked = linkedBankAccounts.stream()
          .anyMatch(
              account -> account.getAccountNumber().equals(accountNumber) && account.getBankCode().equals(bankCode));

      if (!isLinked) {
        log.warn("Bank account not linked - BankCode: {}, AccountNumber: {}", bankCode, accountNumber);
        throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
      }

      BankCommand bankCommand = BankCommand
          .newBuilder()
          .setSagaId(sagaId.toString())
          .setAmount(amount)
          .setBankCode(bankCode)
          .setAccountNumber(accountNumber)
          .setAction(BankAction.WITHDRAW) // Rút tiền từ ngân hàng trước
          .build();

      saveToOutbox("bank-commands", sagaId.toString(), bankCommand);

      Transaction transaction = Transaction.builder()
          .id(sagaId.toString())
          .amount(BigDecimal.valueOf(amount))
          .type(TransactionType.DEPOSIT) // Nạp tiền từ ngân hàng vào ví
          .sourceWalletId(userId.toString())
          .destinationWalletId(null)
          .bankCode(bankCode)
          .accountNumber(accountNumber)
          .build();

      transactionRepository.save(transaction);

      log.info("Bank command saved to outbox for saga ID: {}", sagaId);

      return DepositFromBankResponseDto.builder()
          .transactionId(sagaId.toString())
          .amount(amount)
          .type(TransactionType.DEPOSIT)
          .sourceWalletId(userId.toString())
          .destinationWalletId(null)
          .status(TransactionStatus.PENDING)
          .build();
    }

    catch (AppException e) {
      log.error("AppException in processDepositFromBank: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error processing deposit from bank: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  // Withdraw : Wallet -> Bank : Rút tiền từ ví về ngân hàng
  // Trừ tiền ví trước
  @Override
  public WithdrawToBankResponseDto processWithdrawalFromBank(double amount, String bankCode, String accountNumber) {
    try {
      UUID sagaId = UUID.randomUUID();
      UUID userId = SecurityUtils.getAuthenticatedUserId();

      var linkedAccountsResponse = profileClient.getMyLinkedBankAccounts();
      if (linkedAccountsResponse == null || linkedAccountsResponse.getBody() == null) {
        log.error("Failed to fetch linked bank accounts from profile service");
        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
      }

      List<LinkedBankAccountsReponseDto> linkedBankAccounts = linkedAccountsResponse.getBody().getData();

      var walletBalanceResponse = walletClient.getBalance();
      if (walletBalanceResponse == null || walletBalanceResponse.getBody() == null) {
        log.error("Failed to fetch wallet balance from wallet service");
        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
      }

      WalletBalanceResponseDto walletBalance = walletBalanceResponse.getBody().getData();

      if (linkedBankAccounts == null || linkedBankAccounts.isEmpty()) {
        log.error("User has no linked bank accounts");
        throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
      }

      boolean isLinked = linkedBankAccounts.stream()
          .anyMatch(
              account -> account.getAccountNumber().equals(accountNumber) && account.getBankCode().equals(bankCode));

      if (!isLinked) {
        log.warn("Bank account not linked - BankCode: {}, AccountNumber: {}", bankCode, accountNumber);
        throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
      }

      boolean hasSufficientBalance = walletBalance.getBalance().doubleValue() >= amount;

      if (!hasSufficientBalance) {
        log.warn("Insufficient balance - Current: {}, Required: {}", walletBalance.getBalance(), amount);
        throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
      }

      WalletCommand withdrawWalletCommand = WalletCommand
          .newBuilder()
          .setSagaId(sagaId.toString())
          .setAmount(amount)
          .setUserId(userId.toString())
          .setTransactionType(TransactionWalletCommandType.WITHDRAWAL.toString())
          .setAction(WalletAction.DEBIT) // Trừ tiền ví trước
          .build();

      saveToOutbox("wallet-commands", sagaId.toString(), withdrawWalletCommand);

      Transaction transaction = Transaction.builder()
          .id(sagaId.toString())
          .amount(BigDecimal.valueOf(amount))
          .type(TransactionType.WITHDRAWAL)
          .sourceWalletId(userId.toString())
          .destinationWalletId(null)
          .bankCode(bankCode)
          .accountNumber(accountNumber)
          .build();

      transactionRepository.save(transaction);

      log.info("Wallet command saved to outbox for saga ID: {}", sagaId);

      return WithdrawToBankResponseDto.builder()
          .transactionId(sagaId.toString())
          .amount(amount)
          .type(TransactionType.WITHDRAWAL)
          .sourceWalletId(userId.toString())
          .destinationWalletId(null)
          .status(TransactionStatus.PENDING)
          .build();

    } catch (AppException e) {
      log.error("AppException in processWithdrawalFromBank: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error processing withdrawal to bank: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  private void saveToOutbox(String topic, String sagaId, Object payload) throws Exception {
    OutboxMessage outboxMessage = OutboxMessage.builder()
        .topic(topic)
        .sagaId(sagaId)
        .payload(objectMapper.writeValueAsString(payload))
        .build();
    outboxMessageRepository.save(outboxMessage);
  }

  @Override
  public InternalTransferResponseDto processInternalTransfer(double amount,
      String destinationWalletId) {

    try {
      UUID sagaId = UUID.randomUUID();
      UUID userId = SecurityUtils.getAuthenticatedUserId();

      var walletBalanceResponse = walletClient.getBalance();

      if (walletBalanceResponse == null || walletBalanceResponse.getBody() == null) {
        log.error("Failed to fetch wallet balance from wallet service");
        throw new AppException(ErrorCode.USER_WALLET_NOT_FOUND);
      }

      WalletBalanceResponseDto walletBalance = walletBalanceResponse.getBody().getData();

      boolean hasSufficientBalance = walletBalance.getBalance().doubleValue() >= amount;

      if (!hasSufficientBalance) {
        log.warn("Insufficient balance - Current: {}, Required: {}", walletBalance.getBalance(), amount);
        throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
      }

      var destinationWalletExistResponse = walletClient.isWalletExists(destinationWalletId);

      if (!destinationWalletExistResponse.getBody().getData().isExist()) {
        log.error("Destination wallet does not exist");
        throw new AppException(ErrorCode.DESTINATION_WALLET_NOT_FOUND);
      }

      WalletCommand debitCommand = WalletCommand
          .newBuilder()
          .setSagaId(sagaId.toString())
          .setAmount(amount)
          .setUserId(userId.toString())
          .setTransactionType(TransactionWalletCommandType.TRANSFER.toString())
          .setAction(WalletAction.DEBIT) // Trừ tiền ví nguồn trước
          .setDestinationWalletId(destinationWalletId)
          .build();

      saveToOutbox("wallet-commands", sagaId.toString(), debitCommand);

      Transaction transaction = Transaction.builder()
          .id(sagaId.toString())
          .amount(BigDecimal.valueOf(amount))
          .type(TransactionType.TRANSFER)
          .sourceWalletId(userId.toString())
          .destinationWalletId(destinationWalletId)
          .build();

      transactionRepository.save(transaction);

      log.info("Wallet debit command saved to outbox for saga ID: {}", sagaId);

      return InternalTransferResponseDto.builder()
          .transactionId(sagaId.toString())
          .amount(amount)
          .type(TransactionType.TRANSFER)
          .sourceWalletId(userId.toString())
          .destinationWalletId(destinationWalletId)
          .status(TransactionStatus.PENDING)
          .build();

    } catch (AppException e) {
      log.error("AppException in processInternalTransfer: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error processing internal transfer: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

  }

}
