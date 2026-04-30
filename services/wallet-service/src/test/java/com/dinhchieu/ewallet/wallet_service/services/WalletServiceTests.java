package com.dinhchieu.ewallet.wallet_service.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.wallet_service.enums.TransactionType;
import com.dinhchieu.ewallet.wallet_service.enums.WalletStatus;
import com.dinhchieu.ewallet.wallet_service.enums.WalletTransactionStatus;
import com.dinhchieu.ewallet.wallet_service.models.dtos.response.WalletBalanceResponseDto;
import com.dinhchieu.ewallet.wallet_service.models.dtos.response.WalletExistResponseDto;
import com.dinhchieu.ewallet.wallet_service.models.dtos.response.WalletResultDto;
import com.dinhchieu.ewallet.wallet_service.models.entities.Wallet;
import com.dinhchieu.ewallet.wallet_service.models.entities.WalletTransaction;
import com.dinhchieu.ewallet.wallet_service.repositories.WalletRepository;
import com.dinhchieu.ewallet.wallet_service.repositories.WalletTransactionRepository;
import com.dinhchieu.ewallet.wallet_service.services.Impl.WalletServiceImpl;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTests {
  @Mock
  private WalletRepository walletRepository;

  @Mock
  private WalletTransactionRepository walletTransactionRepository;

  @InjectMocks
  private WalletServiceImpl walletServiceImpl;

  @Test
  @DisplayName("Test getBalance - Success")
  void testGetBalance_success() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    BigDecimal expectedBalance = new BigDecimal("100.00");

    Wallet wallet = Wallet.builder()
        .id(walletId)
        .balance(expectedBalance)
        .build();

    // Act
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

    WalletBalanceResponseDto response = walletServiceImpl.getBalance(walletId);

    // Assert
    Assertions.assertThat(response).isNotNull();
    Assertions.assertThat(response.getWalletId()).isEqualTo(walletId.toString());
    Assertions.assertThat(response.getBalance()).isEqualByComparingTo(expectedBalance);
  }

  @Test
  @DisplayName("Test getBalance - Wallet Not Found")
  void testGetBalance_walletNotFound() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // Act & Assert
    Assertions.assertThatThrownBy(() -> walletServiceImpl.getBalance(walletId))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_WALLET_NOT_FOUND);
  }

  @Test
  @DisplayName("Test isWalletExist - True")
  void testIsWalletExist_true() {
    // Arrange
    UUID userId = UUID.randomUUID();
    when(walletRepository.existsById(userId)).thenReturn(true);

    // Act
    WalletExistResponseDto response = walletServiceImpl.isWalletExist(userId);

    // Assert
    Assertions.assertThat(response.isExist()).isTrue();
  }

  @Test
  @DisplayName("Test isWalletExist - False")
  void testIsWalletExist_false() {
    // Arrange
    UUID userId = UUID.randomUUID();
    when(walletRepository.existsById(userId)).thenReturn(false);

    // Act
    WalletExistResponseDto response = walletServiceImpl.isWalletExist(userId);

    // Assert
    Assertions.assertThat(response.isExist()).isFalse();
  }

  @Test
  @DisplayName("Test activeWallet - New Wallet")
  void testActiveWallet_newWallet() {
    // Arrange
    UUID userId = UUID.randomUUID();
    when(walletRepository.findById(userId)).thenReturn(Optional.empty());

    // Act
    walletServiceImpl.activeWallet(userId);

    // Assert
    verify(walletRepository, times(1)).save(any(Wallet.class));
  }

  @Test
  @DisplayName("Test activeWallet - Wallet Already Active")
  void testActiveWallet_alreadyActive() {
    // Arrange
    UUID userId = UUID.randomUUID();
    Wallet wallet = Wallet.builder()
        .id(userId)
        .status(WalletStatus.ACTIVE)
        .build();
    when(walletRepository.findById(userId)).thenReturn(Optional.of(wallet));

    // Act & Assert
    Assertions.assertThatThrownBy(() -> walletServiceImpl.activeWallet(userId))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_WALLET_ALREADY_ACTIVE);
  }

  @Test
  @DisplayName("Test activeWallet - Invalid State Transition")
  void testActiveWallet_invalidTransition() {
    // Arrange
    UUID userId = UUID.randomUUID();
    Wallet wallet = Wallet.builder()
        .id(userId)
        .status(WalletStatus.FROZEN)
        .build();
    when(walletRepository.findById(userId)).thenReturn(Optional.of(wallet));

    // Act & Assert
    Assertions.assertThatThrownBy(() -> walletServiceImpl.activeWallet(userId))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_WALLET_TRANSITION);
  }

  @Test
  @DisplayName("Test processCredit - Success")
  void testProcessCredit_success() {
    // Arrange
    String sagaId = UUID.randomUUID().toString();
    String walletId = UUID.randomUUID().toString();
    double amount = 50.0;
    Wallet wallet = Wallet.builder()
        .id(UUID.fromString(walletId))
        .balance(new BigDecimal("100.00"))
        .transactions(new ArrayList<>())
        .build();

    when(walletRepository.findById(UUID.fromString(walletId))).thenReturn(Optional.of(wallet));
    when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(i -> {
      WalletTransaction tx = i.getArgument(0);
      tx.setId(UUID.randomUUID());
      return tx;
    });

    // Act
    WalletResultDto result = walletServiceImpl.processCredit(sagaId, amount, walletId, TransactionType.DEPOSIT, null);

    // Assert
    Assertions.assertThat(result.getStatus()).isEqualTo(WalletTransactionStatus.SUCCESS.name());
    Assertions.assertThat(wallet.getBalance()).isEqualByComparingTo("150.00");
  }

  @Test
  @DisplayName("Test processDebit - Success")
  void testProcessDebit_success() {
    // Arrange
    String sagaId = UUID.randomUUID().toString();
    String walletId = UUID.randomUUID().toString();
    double amount = 50.0;
    Wallet wallet = Wallet.builder()
        .id(UUID.fromString(walletId))
        .balance(new BigDecimal("100.00"))
        .transactions(new ArrayList<>())
        .build();

    when(walletRepository.findById(UUID.fromString(walletId))).thenReturn(Optional.of(wallet));
    when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(i -> {
      WalletTransaction tx = i.getArgument(0);
      tx.setId(UUID.randomUUID());
      return tx;
    });

    // Act
    WalletResultDto result = walletServiceImpl.processDebit(sagaId, amount, walletId, TransactionType.WITHDRAWAL, null);

    // Assert
    Assertions.assertThat(result.getStatus()).isEqualTo(WalletTransactionStatus.SUCCESS.name());
    Assertions.assertThat(wallet.getBalance()).isEqualByComparingTo("50.00");
  }

  @Test
  @DisplayName("Test processDebit - Insufficient Balance")
  void testProcessDebit_insufficientBalance() {
    // Arrange
    String sagaId = UUID.randomUUID().toString();
    String walletId = UUID.randomUUID().toString();
    double amount = 150.0;
    Wallet wallet = Wallet.builder()
        .id(UUID.fromString(walletId))
        .balance(new BigDecimal("100.00"))
        .build();

    when(walletRepository.findById(UUID.fromString(walletId))).thenReturn(Optional.of(wallet));

    // Act
    WalletResultDto result = walletServiceImpl.processDebit(sagaId, amount, walletId, TransactionType.WITHDRAWAL, null);

    // Assert
    Assertions.assertThat(result.getStatus()).isEqualTo(WalletTransactionStatus.FAILURE.name());
    Assertions.assertThat(result.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE.getCode());
  }

  @Test
  @DisplayName("Test processCredit - Wallet Not Found")
  void testProcessCredit_walletNotFound() {
    // Arrange
    String sagaId = UUID.randomUUID().toString();
    String walletId = UUID.randomUUID().toString();
    when(walletRepository.findById(UUID.fromString(walletId))).thenReturn(Optional.empty());

    // Act
    WalletResultDto result = walletServiceImpl.processCredit(sagaId, 100.0, walletId, TransactionType.DEPOSIT, null);

    // Assert
    Assertions.assertThat(result.getStatus()).isEqualTo(WalletTransactionStatus.FAILURE.name());
    Assertions.assertThat(result.getErrorCode()).isEqualTo(ErrorCode.USER_WALLET_NOT_FOUND.getCode());
  }

  @Test
  @DisplayName("Test processDebit - Destination Wallet Not Found")
  void testProcessDebit_destWalletNotFound() {
    // Arrange
    String sagaId = UUID.randomUUID().toString();
    String walletId = UUID.randomUUID().toString();
    String destWalletId = UUID.randomUUID().toString();
    Wallet wallet = Wallet.builder()
        .id(UUID.fromString(walletId))
        .balance(new BigDecimal("100.00"))
        .build();

    when(walletRepository.findById(UUID.fromString(walletId))).thenReturn(Optional.of(wallet));
    when(walletRepository.findById(UUID.fromString(destWalletId))).thenReturn(Optional.empty());

    // Act
    WalletResultDto result = walletServiceImpl.processDebit(sagaId, 50.0, walletId, TransactionType.TRANSFER,
        destWalletId);

    // Assert
    Assertions.assertThat(result.getStatus()).isEqualTo(WalletTransactionStatus.FAILURE.name());
    Assertions.assertThat(result.getErrorCode()).isEqualTo(ErrorCode.USER_WALLET_NOT_FOUND.getCode());
  }

  @Test
  @DisplayName("Test processDebit - Success with Transfer")
  void testProcessDebit_transferSuccess() {
    // Arrange
    String sagaId = UUID.randomUUID().toString();
    String walletId = UUID.randomUUID().toString();
    String destWalletId = UUID.randomUUID().toString();
    double amount = 50.0;

    Wallet sourceWallet = Wallet.builder()
        .id(UUID.fromString(walletId))
        .balance(new BigDecimal("100.00"))
        .transactions(new ArrayList<>())
        .build();

    Wallet destWallet = Wallet.builder()
        .id(UUID.fromString(destWalletId))
        .balance(new BigDecimal("20.00"))
        .transactions(new ArrayList<>())
        .build();

    when(walletRepository.findById(UUID.fromString(walletId))).thenReturn(Optional.of(sourceWallet));
    when(walletRepository.findById(UUID.fromString(destWalletId))).thenReturn(Optional.of(destWallet));
    when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(i -> {
      WalletTransaction tx = i.getArgument(0);
      tx.setId(UUID.randomUUID());
      return tx;
    });

    // Act
    WalletResultDto result = walletServiceImpl.processDebit(sagaId, amount, walletId, TransactionType.TRANSFER,
        destWalletId);

    // Assert
    Assertions.assertThat(result.getStatus()).isEqualTo(WalletTransactionStatus.SUCCESS.name());
    Assertions.assertThat(sourceWallet.getBalance()).isEqualByComparingTo("50.00");
    Assertions.assertThat(destWallet.getBalance()).isEqualByComparingTo("70.00");
    verify(walletTransactionRepository, times(2)).save(any(WalletTransaction.class));
  }
}
