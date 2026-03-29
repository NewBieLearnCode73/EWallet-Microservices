package com.dinhchieu.ewallet.bank_adapter_service.sagas.handler;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dinhchieu.ewallet.avro.BankAction;
import com.dinhchieu.ewallet.avro.BankCommand;
import com.dinhchieu.ewallet.avro.TransactionEvent;
import com.dinhchieu.ewallet.bank_adapter_service.dtos.response.BankResult;
import com.dinhchieu.ewallet.bank_adapter_service.sagas.outbox.OutboxMessage;
import com.dinhchieu.ewallet.bank_adapter_service.sagas.outbox.OutboxMessageRepository;
import com.dinhchieu.ewallet.bank_adapter_service.services.BankServiceRouter;
import com.dinhchieu.ewallet.common_library.enums.TransactionEventService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BankKafkaListener {
  private final ObjectMapper objectMapper;
  private final BankServiceRouter bankServiceRouter;
  private final OutboxMessageRepository outboxMessageRepository;

  @KafkaListener(topics = "bank-commands", groupId = "bank-adapter-service")
  @Transactional
  public void handleBankCommand(
      ConsumerRecord<String, BankCommand> record) {
    log.info("Received BankCommand with key: {}, value: {}", record.key(), record.value());

    try {
      BankCommand command = record.value();
      BankResult result;

      if (BankAction.DEPOSIT == command.getAction()) {
        result = bankServiceRouter.processDeposit(command.getSagaId(), command.getBankCode(),
            command.getAmount(), command.getAccountNumber());
      } else if (BankAction.WITHDRAW == command.getAction()) {
        result = bankServiceRouter.processWithdrawal(command.getSagaId(), command.getBankCode(),
            command.getAmount(), command.getAccountNumber());
      } else {
        log.error("Unsupported BankAction: {} in BankCommand with key: {}", command.getAction(), record.key());
        return;
      }

      // Stop sending event if this is a compensation command from Orchestrator
      if (command.getIsCompensation()) {
        log.info("Compensation completed for Bank SagaId: {}. No event will be sent.", command.getSagaId());
        return;
      }

      TransactionEvent transactionEvent = TransactionEvent.newBuilder()
          .setSagaId(command.getSagaId())
          .setReferenceId(result.getTransactionRefId())
          .setServiceName(TransactionEventService.BANK_ADAPTER_SERVICE.name())
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

    } catch (Exception e) {
      log.error("Error processing BankCommand with key: {}, value: {}. Error: {}", record.key(), record.value(),
          e.getMessage(), e);
      throw new RuntimeException("Kafka Retry Triggered due to System Error", e);
    }
  }
}
