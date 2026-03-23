package com.dinhchieu.ewallet.wallet_service.sagas.handler;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dinhchieu.ewallet.avro.TransactionEvent;
import com.dinhchieu.ewallet.avro.WalletAction;
import com.dinhchieu.ewallet.avro.WalletCommand;
import com.dinhchieu.ewallet.common_library.enums.TransactionEventService;
import com.dinhchieu.ewallet.wallet_service.dtos.response.WalletResult;
import com.dinhchieu.ewallet.wallet_service.enums.TransactionType;
import com.dinhchieu.ewallet.wallet_service.sagas.outbox.OutboxMessage;
import com.dinhchieu.ewallet.wallet_service.sagas.outbox.OutboxMessageRepository;
import com.dinhchieu.ewallet.wallet_service.services.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletKafkaListener {
  private final ObjectMapper objectMapper;
  private final WalletService walletService;
  private final OutboxMessageRepository outboxMessageRepository;

  @KafkaListener(topics = "wallet-commands", groupId = "wallet-service")
  @Transactional
  public void handleWalletCommand(
      ConsumerRecord<String, WalletCommand> record) {
    log.info("Received WalletCommand with key: {}, value: {}", record.key(), record.value());

    try {
      WalletCommand command = record.value();
      WalletResult result;
      String destinationWalletId = command.getDestinationWalletId() != null ? command.getDestinationWalletId() : null;

      if (WalletAction.CREDIT == command.getAction()) {
        result = walletService
            .processCredit(
                command.getSagaId(),
                command.getAmount(),
                command.getUserId(),
                TransactionType.valueOf(command.getTransactionType()),
                destinationWalletId);

        log.info("Processing CREDIT action for WalletCommand with key: {}", record.key());
      } else if (WalletAction.DEBIT == command.getAction()) {
        result = walletService
            .processDebit(
                command.getSagaId(),
                command.getAmount(),
                command.getUserId(),
                TransactionType.valueOf(command.getTransactionType()),
                destinationWalletId);

        log.info("Processing DEBIT action for WalletCommand with key: {}", record.key());
      } else {
        log.error("Unsupported WalletAction: {} in WalletCommand with key: {}", command.getAction(), record.key());
        return;
      }

      // Stop sending event if this is a compensation command from Orchestrator
      if (command.getIsCompensation()) {
        log.info("Compensation completed for Wallet SagaId: {}. No event will be sent.", command.getSagaId());
        return;
      }

      TransactionEvent transactionEvent = TransactionEvent.newBuilder()
          .setSagaId(command.getSagaId())
          .setReferenceId(result.getTransactionRefId())
          .setServiceName(TransactionEventService.WALLET_SERVICE.name())
          .setStatus(result.getStatus())
          .setErrorCode(result.getErrorCode() != null ? result.getErrorCode() : null)
          .setErrorMessage(
              result.getErrorMessage() != null && !result.getErrorMessage()
                  .isEmpty() ? result.getErrorMessage() : null)
          .build();

      OutboxMessage outboxMessage = OutboxMessage.builder()
          .topic("transaction-events")
          .sagaId(command.getSagaId().toString())
          .payload(objectMapper.writeValueAsString(transactionEvent))
          .build();

      outboxMessageRepository.save(outboxMessage);
      log.info("Successfully processed command and saved OutboxMessage for SagaId: {}", command.getSagaId());

    } catch (Exception e) {
      log.error("Error processing WalletCommand with key: {}, value: {}, error: {}", record.key(),
          record.value(), e.getMessage(), e);

      throw new RuntimeException("Kafka Retry Triggered due to System Error", e);
    }
  }
}
