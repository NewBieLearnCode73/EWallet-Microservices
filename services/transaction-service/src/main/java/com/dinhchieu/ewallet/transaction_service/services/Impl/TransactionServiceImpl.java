package com.dinhchieu.ewallet.transaction_service.services.Impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.dinhchieu.ewallet.transaction_service.models.dtos.request.TransactionSearchRequestDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.DepositFromBankResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.InternalTransferResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.TransactionResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.TransactionSearchResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.dtos.response.WithdrawToBankResponseDto;
import com.dinhchieu.ewallet.transaction_service.models.entities.Transaction;
import com.dinhchieu.ewallet.transaction_service.models.entities.TransactionDocument;
import com.dinhchieu.ewallet.transaction_service.repositories.jpa.TransactionRepository;
import com.dinhchieu.ewallet.transaction_service.repositories.mongo.TransactionDocumentRepository;
import com.dinhchieu.ewallet.transaction_service.sagas.outbox.OutboxMessage;
import com.dinhchieu.ewallet.transaction_service.sagas.outbox.OutboxMessageRepository;
import com.dinhchieu.ewallet.transaction_service.services.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.transaction.annotation.Transactional;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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
  private final TransactionDocumentRepository transactionDocumentRepository;
  private final MongoTemplate mongoTemplate;

  @Override
  public TransactionResponseDto getTransactionById(String transactionId) {
    var transactionOpt = transactionDocumentRepository.findById(transactionId);
    if (transactionOpt.isEmpty()) {
      throw new AppException(ErrorCode.TRANSACTION_NOT_FOUND);
    }

    var transactionDocument = transactionOpt.get();

    return TransactionResponseDto.builder()
        .transactionId(transactionDocument.getId())
        .amount(transactionDocument.getAmount())
        .type(transactionDocument.getType() != null ? transactionDocument.getType().name() : null)
        .sourceWalletId(transactionDocument.getSourceWalletId())
        .sourceUserName(transactionDocument.getSourceUserName())
        .destinationWalletId(transactionDocument.getDestinationWalletId())
        .destinationUserName(transactionDocument.getDestinationUserName())
        .description(transactionDocument.getDescription())
        .status(transactionDocument.getStatus())
        .createdAt(transactionDocument.getCreatedAt())
        .updatedAt(transactionDocument.getUpdatedAt())
        .build();
  }

  @Override
  public TransactionSearchResponseDto searchTransactions(String userId, TransactionSearchRequestDto searchRequest) {
    try {
      // Set default values
      int page = searchRequest.getPage() != null ? searchRequest.getPage() : 0;
      int pageSize = searchRequest.getPageSize() != null ? searchRequest.getPageSize() : 10;
      String sortBy = searchRequest.getSortBy() != null ? searchRequest.getSortBy() : "createdAt";
      String sortOrder = searchRequest.getSortOrder() != null ? searchRequest.getSortOrder() : "DESC";

      // Create Sort object
      Sort.Direction direction = sortOrder.toUpperCase().equals("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
      Sort sort = Sort.by(direction, sortBy);

      // Create Pageable object
      Pageable pageable = PageRequest.of(page, pageSize, sort);

      // User can only search their own wallet - ignore walletId from request
      String walletId = userId;

      TransactionType typeEnum = null;
      if (searchRequest.getTransactionType() != null && !searchRequest.getTransactionType().isEmpty()) {
        try {
          typeEnum = TransactionType.valueOf(searchRequest.getTransactionType().toUpperCase());
        } catch (IllegalArgumentException e) {
          throw new AppException(ErrorCode.INVALID_INPUT);
        }
      }

      TransactionStatus statusEnum = null;
      if (searchRequest.getStatus() != null && !searchRequest.getStatus().isEmpty()) {
        try {
          statusEnum = TransactionStatus.valueOf(searchRequest.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
          throw new AppException(ErrorCode.INVALID_INPUT);
        }
      }

      Query query = new Query();

      // Filter by wallet ID (source or destination)
      Criteria walletCriteria = new Criteria().orOperator(
          Criteria.where("sourceWalletId").is(walletId),
          Criteria.where("destinationWalletId").is(walletId)
      );
      query.addCriteria(walletCriteria);

      if (statusEnum != null) {
        query.addCriteria(Criteria.where("status").is(statusEnum));
      }

      if (typeEnum != null) {
        query.addCriteria(Criteria.where("type").is(typeEnum));
      }

      long total = mongoTemplate.count(query, TransactionDocument.class);
      query.with(pageable);
      List<TransactionDocument> transactionDocuments = mongoTemplate.find(query, TransactionDocument.class);
      Page<TransactionDocument> transactionsPage = new PageImpl<>(transactionDocuments, pageable, total);

      // Convert to response DTOs
      List<TransactionResponseDto> transactionResponses = transactionsPage.getContent()
          .stream()
          .map(doc -> TransactionResponseDto.builder()
              .transactionId(doc.getId())
              .amount(doc.getAmount())
              .type(doc.getType() != null ? doc.getType().name() : null)
              .sourceWalletId(doc.getSourceWalletId())
              .sourceUserName(doc.getSourceUserName())
              .destinationWalletId(doc.getDestinationWalletId())
              .destinationUserName(doc.getDestinationUserName())
              .description(doc.getDescription())
              .status(doc.getStatus())
              .createdAt(doc.getCreatedAt())
              .updatedAt(doc.getUpdatedAt())
              .build())
          .toList();

      log.info("Search transactions for user: {}, found: {} results", userId, transactionsPage.getTotalElements());

      return TransactionSearchResponseDto.builder()
          .transactions(transactionResponses)
          .currentPage(page)
          .pageSize(pageSize)
          .totalElements(transactionsPage.getTotalElements())
          .totalPages(transactionsPage.getTotalPages())
          .isFirst(transactionsPage.isFirst())
          .isLast(transactionsPage.isLast())
          .hasNext(transactionsPage.hasNext())
          .hasPrevious(transactionsPage.hasPrevious())
          .build();
    } catch (AppException e) {
      log.error("AppException in searchTransactions for user: {}", userId, e);
      throw e;
    } catch (Exception e) {
      log.error("Error searching transactions for user: {}", userId, e);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  @Override
  public TransactionSearchResponseDto adminSearchAllTransactions(TransactionSearchRequestDto searchRequest) {
    try {
      // Set default values
      int page = searchRequest.getPage() != null ? searchRequest.getPage() : 0;
      int pageSize = searchRequest.getPageSize() != null ? searchRequest.getPageSize() : 10;
      String sortBy = searchRequest.getSortBy() != null ? searchRequest.getSortBy() : "createdAt";
      String sortOrder = searchRequest.getSortOrder() != null ? searchRequest.getSortOrder() : "DESC";

      // Create Sort object
      Sort.Direction direction = sortOrder.toUpperCase().equals("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
      Sort sort = Sort.by(direction, sortBy);

      // Create Pageable object
      Pageable pageable = PageRequest.of(page, pageSize, sort);

      TransactionType typeEnum = null;
      if (searchRequest.getTransactionType() != null && !searchRequest.getTransactionType().isEmpty()) {
        try {
          typeEnum = TransactionType.valueOf(searchRequest.getTransactionType().toUpperCase());
        } catch (IllegalArgumentException e) {
          throw new AppException(ErrorCode.INVALID_INPUT);
        }
      }

      TransactionStatus statusEnum = null;
      if (searchRequest.getStatus() != null && !searchRequest.getStatus().isEmpty()) {
        try {
          statusEnum = TransactionStatus.valueOf(searchRequest.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
          throw new AppException(ErrorCode.INVALID_INPUT);
        }
      }

      Query query = new Query();

      // If walletId is provided, search for that wallet
      if (searchRequest.getWalletId() != null && !searchRequest.getWalletId().isEmpty()) {
        Criteria walletCriteria = new Criteria().orOperator(
            Criteria.where("sourceWalletId").is(searchRequest.getWalletId()),
            Criteria.where("destinationWalletId").is(searchRequest.getWalletId())
        );
        query.addCriteria(walletCriteria);
      }

      if (statusEnum != null) {
        query.addCriteria(Criteria.where("status").is(statusEnum));
      }

      if (typeEnum != null) {
        query.addCriteria(Criteria.where("type").is(typeEnum));
      }

      long total = mongoTemplate.count(query, TransactionDocument.class);
      query.with(pageable);
      List<TransactionDocument> transactionDocuments = mongoTemplate.find(query, TransactionDocument.class);
      Page<TransactionDocument> transactionsPage = new PageImpl<>(transactionDocuments, pageable, total);

      // Convert to response DTOs
      List<TransactionResponseDto> transactionResponses = transactionsPage.getContent()
          .stream()
          .map(doc -> TransactionResponseDto.builder()
              .transactionId(doc.getId())
              .amount(doc.getAmount())
              .type(doc.getType() != null ? doc.getType().name() : null)
              .sourceWalletId(doc.getSourceWalletId())
              .sourceUserName(doc.getSourceUserName())
              .destinationWalletId(doc.getDestinationWalletId())
              .destinationUserName(doc.getDestinationUserName())
              .description(doc.getDescription())
              .status(doc.getStatus())
              .createdAt(doc.getCreatedAt())
              .updatedAt(doc.getUpdatedAt())
              .build())
          .toList();

      log.info("Admin search transactions, found: {} results", transactionsPage.getTotalElements());

      return TransactionSearchResponseDto.builder()
          .transactions(transactionResponses)
          .currentPage(page)
          .pageSize(pageSize)
          .totalElements(transactionsPage.getTotalElements())
          .totalPages(transactionsPage.getTotalPages())
          .isFirst(transactionsPage.isFirst())
          .isLast(transactionsPage.isLast())
          .hasNext(transactionsPage.hasNext())
          .hasPrevious(transactionsPage.hasPrevious())
          .build();
    } catch (AppException e) {
      log.error("AppException in adminSearchAllTransactions", e);
      throw e;
    } catch (Exception e) {
      log.error("Error in admin search all transactions", e);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  // Deposit : Bank -> Wallet : Nạp tiền từ ngân hàng vào ví
  // Trừ tiền ngân hàng trước
  @Override
  @Retry(name = "transactionServiceRetry", fallbackMethod = "depositFromBankFallback")
  @CircuitBreaker(name = "transactionServiceCircuitBreaker", fallbackMethod = "depositFromBankFallback")
  @Transactional
  public DepositFromBankResponseDto processDepositFromBank(double amount, String bankCode, String accountNumber) {

    try {
      UUID sagaId = UUID.randomUUID();
      UUID userId = SecurityUtils.getAuthenticatedUserId();
      SecurityContext context = SecurityContextHolder.getContext();
      String walletUsername;

      try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

        var delegatedExecutor = new DelegatingSecurityContextExecutorService(executor, context);

        CompletableFuture<?> linkedBankAccountsFuture = CompletableFuture.runAsync(() -> {
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
                  account -> account.getAccountNumber().equals(accountNumber)
                      && account.getBankCode().equals(bankCode));

          if (!isLinked) {
            log.warn("Bank account not linked - BankCode: {}, AccountNumber: {}", bankCode, accountNumber);
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
          }
        }, delegatedExecutor);

        CompletableFuture<String> walletUsernameFuture = CompletableFuture.supplyAsync(() -> {
          var response = profileClient.getProfileFullNameByUserId(userId.toString());
          if (response == null || response.getBody() == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
          }
          return response.getBody().getData().getFullName();
        }, delegatedExecutor);

        try {
          CompletableFuture.allOf(linkedBankAccountsFuture, walletUsernameFuture).get();

          walletUsername = walletUsernameFuture.get();
        } catch (ExecutionException e) {

          log.error("Error when calling profile-service", e.getCause());

          if (e.getCause() instanceof AppException appException) {
            throw appException;
          }
          throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
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

      TransactionDocument transactionDocument = TransactionDocument.builder()
          .id(sagaId.toString())
          .amount(BigDecimal.valueOf(amount))
          .type(TransactionType.DEPOSIT) // Nạp tiền từ ngân hàng vào ví
          .sourceWalletId(userId.toString())
          .sourceUserName(walletUsername)
          .status(TransactionStatus.PENDING)
          .build();

      transactionDocumentRepository.save(transactionDocument);

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
  @Retry(name = "transactionServiceRetry", fallbackMethod = "withdrawFromBankFallback")
  @CircuitBreaker(name = "transactionServiceCircuitBreaker", fallbackMethod = "withdrawFromBankFallback")
  @Transactional
  public WithdrawToBankResponseDto processWithdrawalFromBank(double amount, String bankCode, String accountNumber) {
    try {
      UUID sagaId = UUID.randomUUID();
      UUID userId = SecurityUtils.getAuthenticatedUserId();
      String walletUsername;

      SecurityContext context = SecurityContextHolder.getContext();

      try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var delegatedExecutor = new DelegatingSecurityContextExecutorService(executor, context);

        CompletableFuture<?> linkedBankAccountsFuture = CompletableFuture.runAsync(() -> {
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
                  account -> account.getAccountNumber().equals(accountNumber)
                      && account.getBankCode().equals(bankCode));

          if (!isLinked) {
            log.warn("Bank account not linked - BankCode: {}, AccountNumber: {}", bankCode, accountNumber);
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
          }
        }, delegatedExecutor);

        CompletableFuture<?> balanceFuture = CompletableFuture.runAsync(() -> {
          var walletBalanceResponse = walletClient.getBalance();

          if (walletBalanceResponse == null || walletBalanceResponse.getBody() == null) {
            log.error("Failed to fetch wallet balance from wallet service");
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
          }

          WalletBalanceResponseDto walletBalance = walletBalanceResponse.getBody().getData();

          boolean hasSufficientBalance = walletBalance.getBalance().doubleValue() >= amount;

          if (!hasSufficientBalance) {
            log.warn("Insufficient balance - Current: {}, Required: {}", walletBalance.getBalance(), amount);
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
          }
        }, delegatedExecutor);

        CompletableFuture<String> walletUsernameFuture = CompletableFuture.supplyAsync(() -> {
          var response = profileClient.getProfileFullNameByUserId(userId.toString());
          if (response == null || response.getBody() == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
          }
          return response.getBody().getData().getFullName();
        }, delegatedExecutor);

        try {
          CompletableFuture.allOf(linkedBankAccountsFuture, balanceFuture, walletUsernameFuture).get();

          walletUsername = walletUsernameFuture.get();
        } catch (ExecutionException e) {
          if (e.getCause() instanceof AppException appException) {
            throw appException;
          }
          throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
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

      TransactionDocument transactionDocument = TransactionDocument.builder()
          .id(sagaId.toString())
          .amount(BigDecimal.valueOf(amount))
          .type(TransactionType.WITHDRAWAL)
          .sourceWalletId(userId.toString())
          .sourceUserName(walletUsername)
          .status(TransactionStatus.PENDING)
          .build();

      transactionDocumentRepository.save(transactionDocument);

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

  @Override
  @Retry(name = "transactionServiceRetry", fallbackMethod = "internalTransferFallback")
  @CircuitBreaker(name = "transactionServiceCircuitBreaker", fallbackMethod = "internalTransferFallback")
  @Transactional
  public InternalTransferResponseDto processInternalTransfer(double amount,
      String destinationWalletId) {

    try {
      UUID sagaId = UUID.randomUUID();
      UUID userId = SecurityUtils.getAuthenticatedUserId();
      SecurityContext context = SecurityContextHolder.getContext();
      String walletUsername;
      String destinationWalletUsername;

      // Parallel calls to check balance and destination wallet existence
      try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var delegatedExecutor = new DelegatingSecurityContextExecutorService(executor, context);

        CompletableFuture<String> walletUsernameFuture = CompletableFuture.supplyAsync(() -> {
          var response = profileClient.getProfileFullNameByUserId(userId.toString());
          if (response == null || response.getBody() == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
          }
          return response.getBody().getData().getFullName();
        }, delegatedExecutor);

        CompletableFuture<String> destinationWalletUsernameFuture = CompletableFuture.supplyAsync(() -> {
          var response = profileClient.getProfileFullNameByUserId(destinationWalletId);
          if (response == null || response.getBody() == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
          }
          return response.getBody().getData().getFullName();
        }, delegatedExecutor);

        CompletableFuture<?> balanceFuture = CompletableFuture.runAsync(() -> {
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
        }, delegatedExecutor);

        CompletableFuture<?> destinationWalletFuture = CompletableFuture.runAsync(() -> {
          var destinationWalletExistResponse = walletClient.isWalletExists(destinationWalletId);

          if (destinationWalletExistResponse == null || destinationWalletExistResponse.getBody() == null
              || !destinationWalletExistResponse.getBody().getData().isExist()) {
            log.error("Destination wallet does not exist");
            throw new AppException(ErrorCode.DESTINATION_WALLET_NOT_FOUND);
          }
        }, delegatedExecutor);

        try {
          CompletableFuture
              .allOf(balanceFuture, destinationWalletFuture, walletUsernameFuture, destinationWalletUsernameFuture)
              .get();

          walletUsername = walletUsernameFuture.get();
          destinationWalletUsername = destinationWalletUsernameFuture.get();
        } catch (ExecutionException e) {
          if (e.getCause() instanceof AppException appException) {
            throw appException;
          }
          throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
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

      TransactionDocument transactionDocument = TransactionDocument.builder()
          .id(sagaId.toString())
          .amount(BigDecimal.valueOf(amount))
          .type(TransactionType.TRANSFER)
          .sourceWalletId(userId.toString())
          .sourceUserName(walletUsername)
          .destinationWalletId(destinationWalletId)
          .destinationUserName(destinationWalletUsername)
          .status(TransactionStatus.PENDING)
          .build();

      transactionDocumentRepository.save(transactionDocument);

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

  private void saveToOutbox(String topic, String sagaId, Object payload) throws Exception {
    OutboxMessage outboxMessage = OutboxMessage.builder()
        .topic(topic)
        .sagaId(sagaId)
        .payload(objectMapper.writeValueAsString(payload))
        .build();
    outboxMessageRepository.save(outboxMessage);
  }

  public DepositFromBankResponseDto depositFromBankFallback(double amount, String bankCode, String accountNumber,
      Throwable t) {
    log.error("DEPOSIT FROM BANK FALLBACK: {}", t.getMessage());
    throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR);
  }

  public WithdrawToBankResponseDto withdrawFromBankFallback(double amount, String bankCode, String accountNumber,
      Throwable t) {
    log.error("WITHDRAWAL FROM BANK FALLBACK: {}", t.getMessage());
    throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR);
  }

  public InternalTransferResponseDto internalTransferFallback(double amount, String destinationWalletId, Throwable t) {
    log.error("INTERNAL TRANSFER FALLBACK: {}", t.getMessage());
    throw new AppException(ErrorCode.EXTERNAL_SERVICE_ERROR);
  }
}