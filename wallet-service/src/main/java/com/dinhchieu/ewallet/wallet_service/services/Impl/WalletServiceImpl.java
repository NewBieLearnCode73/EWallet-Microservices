package com.dinhchieu.ewallet.wallet_service.services.Impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dinhchieu.ewallet.common_library.exceptions.AppException;
import com.dinhchieu.ewallet.common_library.exceptions.ErrorCode;
import com.dinhchieu.ewallet.common_library.utils.SecurityUtils;
import com.dinhchieu.ewallet.wallet_service.dtos.response.WalletBalanceResponseDto;
import com.dinhchieu.ewallet.wallet_service.dtos.response.WalletExistResponseDto;
import com.dinhchieu.ewallet.wallet_service.dtos.response.WalletResultDto;
import com.dinhchieu.ewallet.wallet_service.enums.CurrencyType;
import com.dinhchieu.ewallet.wallet_service.enums.TransactionStatus;
import com.dinhchieu.ewallet.wallet_service.enums.TransactionType;
import com.dinhchieu.ewallet.wallet_service.enums.WalletErrorCode;
import com.dinhchieu.ewallet.wallet_service.enums.WalletStatus;
import com.dinhchieu.ewallet.wallet_service.enums.WalletTransactionStatus;
import com.dinhchieu.ewallet.wallet_service.models.entities.Wallet;
import com.dinhchieu.ewallet.wallet_service.models.entities.WalletTransaction;
import com.dinhchieu.ewallet.wallet_service.repositories.WalletRepository;
import com.dinhchieu.ewallet.wallet_service.repositories.WalletTransactionRepository;
import com.dinhchieu.ewallet.wallet_service.services.WalletService;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional
    public WalletResultDto processCredit(
            String sagaId,
            double amount,
            String walletId,
            TransactionType transactionType,
            String destinationWalletId) {

        UUID walletUuid = UUID.fromString(walletId);
        UUID destUuid = destinationWalletId != null ? UUID.fromString(destinationWalletId) : null;
        BigDecimal bdAmount = BigDecimal.valueOf(amount);

        Wallet wallet = walletRepository.findById(walletUuid).orElse(null);
        if (wallet == null) {
            log.warn("Wallet not found: {}", walletId);
            return WalletResultDto.builder()
                    .status(WalletTransactionStatus.FAILURE.name())
                    .errorCode(WalletErrorCode.WALLET_NOT_FOUND.getCode())
                    .errorMessage(WalletErrorCode.WALLET_NOT_FOUND.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        wallet.setBalance(wallet.getBalance().add(bdAmount));

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(bdAmount)
                .transactionType(transactionType)
                .status(TransactionStatus.COMPLETED)
                .sagaId(UUID.fromString(sagaId))
                .destinationWalletId(destUuid)
                .build();
        WalletTransaction savedTx = walletTransactionRepository.save(transaction);

        wallet.getTransactions().add(savedTx);
        walletRepository.save(wallet);

        return WalletResultDto.builder()
                .status(WalletTransactionStatus.SUCCESS.name())
                .transactionRefId(savedTx.getId().toString())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public WalletResultDto processDebit(
            String sagaId,
            double amount,
            String walletId,
            TransactionType transactionType,
            String destinationWalletId) {

        BigDecimal bdAmount = BigDecimal.valueOf(amount);
        UUID sagaUuid = UUID.fromString(sagaId);
        UUID walletUuid = UUID.fromString(walletId);

        UUID destUuid = Optional.ofNullable(destinationWalletId).map(UUID::fromString).orElse(null);

        Optional<Wallet> walletOpt = walletRepository.findById(walletUuid);
        if (walletOpt.isEmpty()) {
            log.warn("Wallet not found: {}", walletId);
            return WalletResultDto.builder()
                    .status(WalletTransactionStatus.FAILURE.name())
                    .errorCode(WalletErrorCode.WALLET_NOT_FOUND.getCode())
                    .errorMessage(WalletErrorCode.WALLET_NOT_FOUND.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        Wallet wallet = walletOpt.get();

        if (wallet.getBalance().compareTo(bdAmount) < 0) {
            log.warn("Insufficient funds. Wallet: {}, Required: {}, Available: {}",
                    walletId, amount, wallet.getBalance());
            return WalletResultDto.builder()
                    .status(WalletTransactionStatus.FAILURE.name())
                    .errorCode(WalletErrorCode.INSUFFICIENT_FUNDS.getCode())
                    .errorMessage(WalletErrorCode.INSUFFICIENT_FUNDS.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        wallet.setBalance(wallet.getBalance().subtract(bdAmount));

        if (destUuid != null) {
            walletRepository.findById(destUuid).ifPresent(destWallet -> {
                destWallet.setBalance(destWallet.getBalance().add(bdAmount));
            });
        }

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(bdAmount.negate())
                .transactionType(transactionType)
                .status(TransactionStatus.COMPLETED)
                .sagaId(sagaUuid)
                .destinationWalletId(destUuid)
                .build();

        WalletTransaction savedTx = walletTransactionRepository.save(transaction);
        wallet.getTransactions().add(savedTx);

        return WalletResultDto.builder()
                .status(WalletTransactionStatus.SUCCESS.name())
                .transactionRefId(savedTx.getId().toString())
                .timestamp(LocalDateTime.now())
                .build();

        // UUID walletUuid = UUID.fromString(walletId);
        // UUID destUuid = destinationWalletId != null ?
        // UUID.fromString(destinationWalletId) : null;
        // BigDecimal bdAmount = BigDecimal.valueOf(amount);

        // Wallet wallet = walletRepository.findById(walletUuid).orElse(null);

        // Wallet destWallet = destUuid != null ?
        // walletRepository.findById(destUuid).orElse(null) : null;

        // if (wallet == null) {
        // log.warn("Wallet not found: {}", walletId);
        // return WalletResultDto.builder()
        // .status(WalletTransactionStatus.FAILURE.name())
        // .errorCode(WalletErrorCode.WALLET_NOT_FOUND.getCode())
        // .errorMessage(WalletErrorCode.WALLET_NOT_FOUND.getMessage())
        // .timestamp(LocalDateTime.now())
        // .build();
        // }

        // if (wallet.getBalance().compareTo(bdAmount) < 0) {
        // log.warn("Insufficient funds. Wallet: {}, Required: {}, Available: {}",
        // walletId, amount,
        // wallet.getBalance());
        // return WalletResultDto.builder()
        // .status(WalletTransactionStatus.FAILURE.name())
        // .errorCode(WalletErrorCode.INSUFFICIENT_FUNDS.getCode())
        // .errorMessage(WalletErrorCode.INSUFFICIENT_FUNDS.getMessage())
        // .timestamp(LocalDateTime.now())
        // .build();
        // }

        // wallet.setBalance(wallet.getBalance().subtract(bdAmount)); // Trừ tiền từ ví
        // if (destWallet != null) {
        // destWallet.setBalance(destWallet.getBalance().add(bdAmount)); // Cộng tiền
        // vào ví đích
        // }

        // WalletTransaction transaction = WalletTransaction.builder()
        // .wallet(wallet)
        // .amount(bdAmount.negate())
        // .transactionType(transactionType)
        // .status(TransactionStatus.COMPLETED)
        // .sagaId(UUID.fromString(sagaId))
        // .destinationWalletId(destUuid)
        // .build();
        // WalletTransaction savedTx = walletTransactionRepository.save(transaction);

        // wallet.getTransactions().add(savedTx);
        // walletRepository.save(wallet);
        // if (destWallet != null) {
        // walletRepository.save(destWallet);
        // }

        // return WalletResultDto.builder()
        // .status(WalletTransactionStatus.SUCCESS.name())
        // .transactionRefId(savedTx.getId().toString())
        // .timestamp(LocalDateTime.now())
        // .build();
    }

    @Override
    public void activeWallet() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();

        Wallet wallet = walletRepository.findById(userId).orElse(null);

        if (wallet != null) {
            WalletStatus status = wallet.getStatus();

            if (status == WalletStatus.ACTIVE) {
                log.warn("Wallet already active for user ID: {}", userId);
                throw new AppException(ErrorCode.USER_WALLET_ALREADY_ACTIVE);
            }

            if (status == WalletStatus.FROZEN || status == WalletStatus.CLOSED) {
                log.warn("Cannot activate wallet with status: {}", status);
                throw new AppException(ErrorCode.INVALID_WALLET_TRANSITION);
            }

            wallet.setStatus(WalletStatus.ACTIVE);
            walletRepository.save(wallet);
            log.info("Wallet activated for user ID: {}", userId);
        } else {
            Wallet newWallet = Wallet.builder()
                    .id(userId)
                    .balance(BigDecimal.ZERO)
                    .currency(CurrencyType.VND)
                    .status(WalletStatus.ACTIVE)
                    .build();
            walletRepository.save(newWallet);
            log.info("Wallet created and activated for user ID: {}", userId);
        }
    }

    @Override
    public WalletBalanceResponseDto getBalance() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();

        Wallet wallet = walletRepository.findById(userId).orElse(null);

        if (wallet == null) {
            log.warn("Wallet not found for user ID: {}", userId);
            throw new AppException(ErrorCode.USER_WALLET_NOT_FOUND);
        }

        log.info("Retrieved balance for user ID: {}: {}", userId, wallet.getBalance());
        return WalletBalanceResponseDto.builder()
                .walletId(wallet.getId().toString())
                .balance(wallet.getBalance())
                .build();
    }

    @Override
    public WalletExistResponseDto isWalletExist(String userId) {
        UUID userUuid = UUID.fromString(userId);
        boolean exist = walletRepository.existsById(userUuid);
        return WalletExistResponseDto.builder()
                .exist(exist)
                .build();
    }
}
