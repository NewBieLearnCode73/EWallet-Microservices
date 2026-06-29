package com.dinhchieu.ewallet.transaction_service.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import com.dinhchieu.ewallet.common_library.dtos.BaseResponse;
import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.common_library.utils.SecurityUtils;
import com.dinhchieu.ewallet.transaction_service.clients.ProfileClient;
import com.dinhchieu.ewallet.transaction_service.clients.WalletClient;
import com.dinhchieu.ewallet.transaction_service.clients.dtos.response.LinkedBankAccountsReponseDto;
import com.dinhchieu.ewallet.transaction_service.clients.dtos.response.ProfileFullNameResponseDto;
import com.dinhchieu.ewallet.transaction_service.clients.dtos.response.WalletBalanceResponseDto;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionStatus;
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
import com.dinhchieu.ewallet.transaction_service.services.Impl.TransactionServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTests {
  @Mock
  private WalletClient walletClient;

  @Mock
  private ProfileClient profileClient;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private OutboxMessageRepository outboxMessageRepository;

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private TransactionDocumentRepository transactionDocumentRepository;

  @Mock
  private MongoTemplate mongoTemplate;

  @InjectMocks
  private TransactionServiceImpl transactionServiceImpl;

  @org.junit.jupiter.api.BeforeEach
  void setUp() throws Exception {
    lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");
  }

  @Test
  @DisplayName("Test getTransactionById - Success")
  void testGetTransactionById_Success() {
    String transactionId = UUID.randomUUID().toString();
    TransactionDocument document = TransactionDocument.builder()
        .id(transactionId)
        .amount(BigDecimal.valueOf(100.0))
        .sourceWalletId("user1")
        .build();

    when(transactionDocumentRepository.findById(transactionId)).thenReturn(Optional.of(document));

    TransactionResponseDto response = transactionServiceImpl.getTransactionById(transactionId);

    Assertions.assertThat(response.getTransactionId()).isEqualTo(transactionId);
    Assertions.assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.0));
  }

  @Test
  @DisplayName("Test getTransactionById - Not Found")
  void testGetTransactionById_NotFound() {
    String transactionId = UUID.randomUUID().toString();
    when(transactionDocumentRepository.findById(transactionId)).thenReturn(Optional.empty());

    Assertions.assertThatThrownBy(() -> transactionServiceImpl.getTransactionById(transactionId))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.TRANSACTION_NOT_FOUND);
  }

  @Test
  @DisplayName("Test searchTransactions - Success")
  void testSearchTransactions_Success() {
    String userId = "user1";
    TransactionSearchRequestDto request = new TransactionSearchRequestDto();
    TransactionDocument doc = TransactionDocument.builder().id("tx1").build();

    when(mongoTemplate.count(any(Query.class), eq(TransactionDocument.class))).thenReturn(1L);
    when(mongoTemplate.find(any(Query.class), eq(TransactionDocument.class))).thenReturn(List.of(doc));

    TransactionSearchResponseDto response = transactionServiceImpl.searchTransactions(userId, request);

    Assertions.assertThat(response.getTransactions()).hasSize(1);
    Assertions.assertThat(response.getTotalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("Test adminSearchAllTransactions - Success")
  void testAdminSearchAllTransactions_Success() {
    TransactionSearchRequestDto request = new TransactionSearchRequestDto();
    TransactionDocument doc = TransactionDocument.builder().id("tx1").build();

    when(mongoTemplate.count(any(Query.class), eq(TransactionDocument.class))).thenReturn(1L);
    when(mongoTemplate.find(any(Query.class), eq(TransactionDocument.class))).thenReturn(List.of(doc));

    TransactionSearchResponseDto response = transactionServiceImpl.adminSearchAllTransactions(request);

    Assertions.assertThat(response.getTransactions()).hasSize(1);
  }

  @Test
  @DisplayName("Test processDepositFromBank - Success")
  void testProcessDepositFromBank_Success() throws Exception {
    double amount = 100.0;
    String bankCode = "VCB";
    String accountNumber = "123456789";
    UUID userId = UUID.randomUUID();

    try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class);
        MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {

      mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);

      SecurityContext securityContext = mock(SecurityContext.class);
      lenient().when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));
      mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

      SecurityContextHolderStrategy strategy = mock(SecurityContextHolderStrategy.class);
      lenient().when(strategy.createEmptyContext()).thenReturn(mock(SecurityContext.class));
      mockedSecurityContextHolder.when(SecurityContextHolder::getContextHolderStrategy).thenReturn(strategy);

      LinkedBankAccountsReponseDto linkedAccount = new LinkedBankAccountsReponseDto(null, bankCode, accountNumber);
      BaseResponse<List<LinkedBankAccountsReponseDto>> profileResponse = BaseResponse
          .<List<LinkedBankAccountsReponseDto>>builder()
          .data(List.of(linkedAccount))
          .build();
      lenient().when(profileClient.getMyLinkedBankAccounts())
          .thenReturn(ResponseEntity.of(Optional.of(profileResponse)));

      ProfileFullNameResponseDto fullNameDto = ProfileFullNameResponseDto.builder().fullName("John Doe").build();
      BaseResponse<ProfileFullNameResponseDto> fullNameResponse = BaseResponse.<ProfileFullNameResponseDto>builder()
          .data(fullNameDto)
          .build();
      lenient().when(profileClient.getProfileFullNameByUserId(anyString()))
          .thenReturn(ResponseEntity.of(Optional.of(fullNameResponse)));

      DepositFromBankResponseDto response = transactionServiceImpl.processDepositFromBank(amount, bankCode,
          accountNumber);

      Assertions.assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING);
      Assertions.assertThat(response.getAmount()).isEqualTo(amount);
      verify(transactionRepository, times(1)).save(any(Transaction.class));
      verify(transactionDocumentRepository, times(1)).save(any(TransactionDocument.class));
      verify(outboxMessageRepository, times(1)).save(any(OutboxMessage.class));
    } catch (AppException e) {
      System.out.println("AppException ErrorCode: " + e.getErrorCode());
      throw e;
    }
  }

  @Test
  @DisplayName("Test processWithdrawalFromBank - Success")
  void testProcessWithdrawalFromBank_Success() throws Exception {
    double amount = 50.0;
    String bankCode = "VCB";
    String accountNumber = "123456789";
    UUID userId = UUID.randomUUID();

    try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class);
        MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {

      mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
      SecurityContext securityContext = mock(SecurityContext.class);
      lenient().when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));
      mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

      SecurityContextHolderStrategy strategy = mock(SecurityContextHolderStrategy.class);
      lenient().when(strategy.createEmptyContext()).thenReturn(mock(SecurityContext.class));
      mockedSecurityContextHolder.when(SecurityContextHolder::getContextHolderStrategy).thenReturn(strategy);

      LinkedBankAccountsReponseDto linkedAccount = new LinkedBankAccountsReponseDto(null, bankCode, accountNumber);
      BaseResponse<List<LinkedBankAccountsReponseDto>> profileResponse = BaseResponse
          .<List<LinkedBankAccountsReponseDto>>builder()
          .data(List.of(linkedAccount))
          .build();
      lenient().when(profileClient.getMyLinkedBankAccounts())
          .thenReturn(ResponseEntity.of(Optional.of(profileResponse)));

      WalletBalanceResponseDto balanceDto = WalletBalanceResponseDto.builder().balance(BigDecimal.valueOf(100.0))
          .build();
      BaseResponse<WalletBalanceResponseDto> walletResponse = BaseResponse.<WalletBalanceResponseDto>builder()
          .data(balanceDto)
          .build();
      lenient().when(walletClient.getBalance()).thenReturn(ResponseEntity.of(Optional.of(walletResponse)));

      ProfileFullNameResponseDto fullNameDto = ProfileFullNameResponseDto.builder().fullName("John Doe").build();
      BaseResponse<ProfileFullNameResponseDto> fullNameResponse = BaseResponse.<ProfileFullNameResponseDto>builder()
          .data(fullNameDto)
          .build();
      lenient().when(profileClient.getProfileFullNameByUserId(anyString()))
          .thenReturn(ResponseEntity.of(Optional.of(fullNameResponse)));

      WithdrawToBankResponseDto response = transactionServiceImpl.processWithdrawalFromBank(amount, bankCode,
          accountNumber);

      Assertions.assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING);
      verify(transactionRepository, times(1)).save(any(Transaction.class));
    } catch (AppException e) {
      System.out.println("AppException ErrorCode: " + e.getErrorCode());
      throw e;
    }
  }

  @Test
  @DisplayName("Test processInternalTransfer - Success")
  void testProcessInternalTransfer_Success() throws Exception {
    double amount = 30.0;
    String destinationWalletId = UUID.randomUUID().toString();
    UUID userId = UUID.randomUUID();

    try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class);
        MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {

      mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
      SecurityContext securityContext = mock(SecurityContext.class);
      lenient().when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));
      mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

      SecurityContextHolderStrategy strategy = mock(SecurityContextHolderStrategy.class);
      lenient().when(strategy.createEmptyContext()).thenReturn(mock(SecurityContext.class));
      mockedSecurityContextHolder.when(SecurityContextHolder::getContextHolderStrategy).thenReturn(strategy);

      ProfileFullNameResponseDto fullNameDto = ProfileFullNameResponseDto.builder().fullName("John Doe").build();
      BaseResponse<ProfileFullNameResponseDto> fullNameResponse = BaseResponse.<ProfileFullNameResponseDto>builder()
          .data(fullNameDto)
          .build();
      lenient().when(profileClient.getProfileFullNameByUserId(anyString()))
          .thenReturn(ResponseEntity.of(Optional.of(fullNameResponse)));

      WalletBalanceResponseDto balanceDto = WalletBalanceResponseDto.builder().balance(BigDecimal.valueOf(100.0))
          .build();
      BaseResponse<WalletBalanceResponseDto> walletResponse = BaseResponse.<WalletBalanceResponseDto>builder()
          .data(balanceDto)
          .build();
      lenient().when(walletClient.getBalance()).thenReturn(ResponseEntity.of(Optional.of(walletResponse)));

      var walletExistResponse = BaseResponse.<com.dinhchieu.ewallet.transaction_service.clients.dtos.response.WalletExistResponseDto>builder()
          .data(new com.dinhchieu.ewallet.transaction_service.clients.dtos.response.WalletExistResponseDto(true))
          .build();
      lenient().when(walletClient.isWalletExists(anyString()))
          .thenReturn(ResponseEntity.of(Optional.of(walletExistResponse)));

      InternalTransferResponseDto response = transactionServiceImpl.processInternalTransfer(amount,
          destinationWalletId);

      Assertions.assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING);
      verify(transactionRepository, times(1)).save(any(Transaction.class));
    } catch (AppException e) {
      System.out.println("AppException ErrorCode: " + e.getErrorCode());
      throw e;
    }
  }

  @Test
  @DisplayName("Test processDepositFromBank - Bank Account Not Found")
  void testProcessDepositFromBank_BankAccountNotFound() throws Exception {
    double amount = 100.0;
    String bankCode = "VCB";
    String accountNumber = "UNKNOWN";
    UUID userId = UUID.randomUUID();

    try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class);
        MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {

      mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
      SecurityContext securityContext = mock(SecurityContext.class);
      mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
      SecurityContextHolderStrategy strategy = mock(SecurityContextHolderStrategy.class);
      lenient().when(strategy.createEmptyContext()).thenReturn(mock(SecurityContext.class));
      mockedSecurityContextHolder.when(SecurityContextHolder::getContextHolderStrategy).thenReturn(strategy);

      LinkedBankAccountsReponseDto linkedAccount = new LinkedBankAccountsReponseDto(null, bankCode, "123");
      BaseResponse<List<LinkedBankAccountsReponseDto>> profileResponse = BaseResponse
          .<List<LinkedBankAccountsReponseDto>>builder()
          .data(List.of(linkedAccount))
          .build();
      lenient().when(profileClient.getMyLinkedBankAccounts())
          .thenReturn(ResponseEntity.of(Optional.of(profileResponse)));

      Assertions
          .assertThatThrownBy(() -> transactionServiceImpl.processDepositFromBank(amount, bankCode, accountNumber))
          .isInstanceOf(AppException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
    }
  }

  @Test
  @DisplayName("Test processWithdrawalFromBank - Insufficient Balance")
  void testProcessWithdrawalFromBank_InsufficientBalance() throws Exception {
    double amount = 1000.0;
    String bankCode = "VCB";
    String accountNumber = "123";
    UUID userId = UUID.randomUUID();

    try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class);
        MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {

      mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
      SecurityContext securityContext = mock(SecurityContext.class);
      mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
      SecurityContextHolderStrategy strategy = mock(SecurityContextHolderStrategy.class);
      lenient().when(strategy.createEmptyContext()).thenReturn(mock(SecurityContext.class));
      mockedSecurityContextHolder.when(SecurityContextHolder::getContextHolderStrategy).thenReturn(strategy);

      LinkedBankAccountsReponseDto linkedAccount = new LinkedBankAccountsReponseDto(null, bankCode, accountNumber);
      BaseResponse<List<LinkedBankAccountsReponseDto>> profileResponse = BaseResponse
          .<List<LinkedBankAccountsReponseDto>>builder()
          .data(List.of(linkedAccount))
          .build();
      lenient().when(profileClient.getMyLinkedBankAccounts())
          .thenReturn(ResponseEntity.of(Optional.of(profileResponse)));

      WalletBalanceResponseDto balanceDto = WalletBalanceResponseDto.builder().balance(BigDecimal.valueOf(10.0))
          .build();
      BaseResponse<WalletBalanceResponseDto> walletResponse = BaseResponse.<WalletBalanceResponseDto>builder()
          .data(balanceDto)
          .build();
      lenient().when(walletClient.getBalance()).thenReturn(ResponseEntity.of(Optional.of(walletResponse)));

      Assertions
          .assertThatThrownBy(() -> transactionServiceImpl.processWithdrawalFromBank(amount, bankCode, accountNumber))
          .isInstanceOf(AppException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
    }
  }

  @Test
  @DisplayName("Test processInternalTransfer - Destination Wallet Not Found")
  void testProcessInternalTransfer_DestinationWalletNotFound() throws Exception {
    double amount = 30.0;
    String destinationWalletId = "UNKNOWN";
    UUID userId = UUID.randomUUID();

    try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class);
        MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {

      mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
      SecurityContext securityContext = mock(SecurityContext.class);
      mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
      SecurityContextHolderStrategy strategy = mock(SecurityContextHolderStrategy.class);
      lenient().when(strategy.createEmptyContext()).thenReturn(mock(SecurityContext.class));
      mockedSecurityContextHolder.when(SecurityContextHolder::getContextHolderStrategy).thenReturn(strategy);

      ProfileFullNameResponseDto fullNameDto = ProfileFullNameResponseDto.builder().fullName("John Doe").build();
      BaseResponse<ProfileFullNameResponseDto> fullNameResponse = BaseResponse.<ProfileFullNameResponseDto>builder()
          .data(fullNameDto)
          .build();
      lenient().when(profileClient.getProfileFullNameByUserId(anyString()))
          .thenReturn(ResponseEntity.of(Optional.of(fullNameResponse)));

      WalletBalanceResponseDto balanceDto = WalletBalanceResponseDto.builder().balance(BigDecimal.valueOf(100.0))
          .build();
      BaseResponse<WalletBalanceResponseDto> walletResponse = BaseResponse.<WalletBalanceResponseDto>builder()
          .data(balanceDto)
          .build();
      lenient().when(walletClient.getBalance()).thenReturn(ResponseEntity.of(Optional.of(walletResponse)));

      var walletExistResponse = BaseResponse.<com.dinhchieu.ewallet.transaction_service.clients.dtos.response.WalletExistResponseDto>builder()
          .data(new com.dinhchieu.ewallet.transaction_service.clients.dtos.response.WalletExistResponseDto(false))
          .build();
      lenient().when(walletClient.isWalletExists(anyString()))
          .thenReturn(ResponseEntity.of(Optional.of(walletExistResponse)));

      Assertions.assertThatThrownBy(() -> transactionServiceImpl.processInternalTransfer(amount, destinationWalletId))
          .isInstanceOf(AppException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.DESTINATION_WALLET_NOT_FOUND);
    }
  }

}
