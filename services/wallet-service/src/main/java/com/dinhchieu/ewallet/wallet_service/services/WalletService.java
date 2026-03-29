package com.dinhchieu.ewallet.wallet_service.services;

import java.util.UUID;

import com.dinhchieu.ewallet.wallet_service.enums.TransactionType;
import com.dinhchieu.ewallet.wallet_service.models.dtos.response.WalletBalanceResponseDto;
import com.dinhchieu.ewallet.wallet_service.models.dtos.response.WalletExistResponseDto;
import com.dinhchieu.ewallet.wallet_service.models.dtos.response.WalletResultDto;

public interface WalletService {
        /**
         * Process a credit transaction for the wallet.
         * 
         * 
         * @param sagaId              the unique identifier for the transaction saga
         * @param amount              the amount to be credited to the wallet
         * @param walletId            the unique identifier of the wallet to which the
         *                            credit will
         *                            be
         * @param transactionType     the type of the transaction
         * @param destinationWalletId the unique identifier of the destination wallet
         * @return a WalletResult object containing the outcome of the credit
         *         transaction, including status, error codes, and messages if
         *         applicable
         */
        WalletResultDto processCredit(String sagaId, double amount, String walletId, TransactionType transactionType,
                        String destinationWalletId);

        /**
         * Process a debit transaction for the wallet. By subtract the specified amount
         * 
         * @param sagaId              the unique identifier for the transaction saga
         * @param amount              the amount to be debited from the wallet
         * @param walletId            the unique identifier of the wallet from which the
         *                            debit will
         *                            be
         * @param transactionType     the type of the transaction
         * @param destinationWalletId the unique identifier of the destination wallet
         * 
         * @return a WalletResult object containing the outcome of the debit
         *         transaction, including status, error codes, and messages if
         *         applicable
         */
        WalletResultDto processDebit(String sagaId, double amount, String walletId, TransactionType transactionType,
                        String destinationWalletId);

        /**
         * Activate a wallet for a user.
         * 
         * 
         * @return void
         */
        void activeWallet(UUID userId);

        /**
         * Get the current balance of the wallet.
         * 
         * @return a WalletBalanceResponse object containing the current balance of the
         *         wallet
         */
        WalletBalanceResponseDto getBalance(UUID userId);

        /**
         * Check if a wallet exists for the given user ID.
         * 
         * @param userId the ID of the user to check for wallet existence
         * 
         * @return a WalletExistResponse object indicating whether a wallet exists for
         *         the
         * 
         */
        WalletExistResponseDto isWalletExist(UUID userId);
}
