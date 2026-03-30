package com.dinhchieu.ewallet.transaction_service.sagas.orchestrator;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dinhchieu.ewallet.avro.BankAction;
import com.dinhchieu.ewallet.avro.BankCommand;
import com.dinhchieu.ewallet.avro.TransactionEvent;
import com.dinhchieu.ewallet.avro.WalletAction;
import com.dinhchieu.ewallet.avro.WalletCommand;
import com.dinhchieu.ewallet.common_library.enums.TransactionEventService;
import com.dinhchieu.ewallet.transaction_service.enums.EventStatus;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionStatus;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionType;
import com.dinhchieu.ewallet.transaction_service.models.entities.Transaction;
import com.dinhchieu.ewallet.transaction_service.repositories.jpa.TransactionRepository;
import com.dinhchieu.ewallet.transaction_service.sagas.outbox.OutboxMessage;
import com.dinhchieu.ewallet.transaction_service.sagas.outbox.OutboxMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionOrchestrator {
  private final ObjectMapper objectMapper;
  private final OutboxMessageRepository outboxMessageRepository;
  private final TransactionRepository transactionRepository;

  @KafkaListener(topics = "transaction-events", groupId = "transaction-orchestrator-group")
  @Transactional
  public void handleTransactionEvent(ConsumerRecord<String, TransactionEvent> record) {
    TransactionEvent event = record.value();
    log.info("Orchestrator received event: SagaId {}, Service: {}, Status: {}", event.getSagaId(),
        event.getServiceName(), event.getStatus());

    try {
      Transaction saga = transactionRepository.findById(event.getSagaId())
          .orElseThrow(() -> new RuntimeException("Saga not found: " + event.getSagaId()));

      if (saga.getStatus() == TransactionStatus.COMPLETED || saga.getStatus() == TransactionStatus.FAILED
          || saga.getStatus() == TransactionStatus.COMPENSATED) {
        log.info("Saga {} is already {}. Ignoring event.", saga.getId(), saga.getStatus());
        return;
      }
      if (saga.getType() == TransactionType.DEPOSIT) {
        handleDepositFlow(saga, event);
      } else if (saga.getType() == TransactionType.WITHDRAWAL) {
        handleWithdrawFlow(saga, event);
      } else if (saga.getType() == TransactionType.TRANSFER) {
        handleTransferFlow(saga, event);
      }
    } catch (Exception e) {
      log.error("Orchestrator failed to process event for SagaId: {}. Error: {}", event.getSagaId(), e.getMessage());
      throw new RuntimeException("Trigger Retry for Orchestrator", e);
    }
  }

  // Bank -> Wallet : Nạp tiền từ ngân hàng vào ví
  private void handleDepositFlow(Transaction saga, TransactionEvent event) throws Exception {
    String eventService = event.getServiceName();

    if (eventService.equals(TransactionEventService.BANK_ADAPTER_SERVICE.name())) {
      if (event.getStatus().equals(EventStatus.SUCCESS.name())) {
        WalletCommand walletCommand = WalletCommand.newBuilder()
            .setSagaId(event.getSagaId())
            .setAmount(saga.getAmount().doubleValue())
            .setUserId(saga.getSourceWalletId())
            .setTransactionType(saga.getType().name())
            .setAction(WalletAction.CREDIT)
            .build();

        saveToOutbox("wallet-commands", event.getSagaId(), walletCommand);
      } else {
        saga.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(saga);
      }
    } 

    else if (eventService.equals(TransactionEventService.WALLET_SERVICE.name())) {
      if (event.getStatus().equals(EventStatus.SUCCESS.name())) {
        saga.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(saga);
      } else {
        log.warn("Wallet service failed for SagaId: {}. Initiating compensation.", event.getSagaId());

        BankCommand bankCommand = BankCommand.newBuilder()
            .setSagaId(event.getSagaId())
            .setAmount(saga.getAmount().doubleValue())
            .setBankCode(saga.getBankCode())
            .setAccountNumber(saga.getAccountNumber())
            .setAction(BankAction.DEPOSIT)
            .setIsCompensation(true)
            .build();

        saveToOutbox("bank-commands", event.getSagaId(), bankCommand);
        saga.setStatus(TransactionStatus.COMPENSATED);
        transactionRepository.save(saga);
      }
    }
  }

  // Wallet -> Bank : Rút tiền từ ví về ngân hàng
  private void handleWithdrawFlow(Transaction saga, TransactionEvent event) throws Exception {
    String eventService = event.getServiceName();
    if (eventService.equals(TransactionEventService.WALLET_SERVICE.name())) {
      if (event.getStatus().equals(EventStatus.SUCCESS.name())) {
        BankCommand bankCommand = BankCommand.newBuilder()
            .setSagaId(event.getSagaId())
            .setAmount(saga.getAmount().doubleValue())
            .setBankCode(saga.getBankCode())
            .setAccountNumber(saga.getAccountNumber())
            .setAction(BankAction.DEPOSIT)
            .build();

        saveToOutbox("bank-commands", event.getSagaId(), bankCommand);
      } else {
        saga.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(saga);
      }
    }

    else if (eventService.equals(TransactionEventService.BANK_ADAPTER_SERVICE.name())) {
      if (event.getStatus().equals(EventStatus.SUCCESS.name())) {
        saga.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(saga);
      } else {
        log.warn("Bank adapter service failed for SagaId: {}. Initiating compensation.", event.getSagaId());

        WalletCommand walletCommand = WalletCommand.newBuilder()
            .setSagaId(event.getSagaId())
            .setAmount(saga.getAmount().doubleValue())
            .setUserId(saga.getSourceWalletId())
            .setTransactionType(saga.getType().name())
            .setAction(WalletAction.CREDIT) // Hoàn tiền vào ví nếu rút tiền từ ngân hàng về ví thất bại
            .setIsCompensation(true)
            .build();
        saveToOutbox("wallet-commands", event.getSagaId(), walletCommand);
        saga.setStatus(TransactionStatus.COMPENSATED);
        transactionRepository.save(saga);
      }
    }
  }

  // Wallet -> Wallet : Chuyển tiền giữa 2 ví
  private void handleTransferFlow(Transaction saga, TransactionEvent event) throws Exception {
    String eventService = event.getServiceName();

    if (eventService.equals(TransactionEventService.WALLET_SERVICE.name())) {
      if (event.getStatus().equals(EventStatus.SUCCESS.name())) {
        saga.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(saga);
      } else {
        saga.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(saga);
      }
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
}
