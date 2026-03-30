package com.dinhchieu.ewallet.transaction_service.sagas.handler;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.dinhchieu.ewallet.avro.TransactionEvent;
import com.dinhchieu.ewallet.common_library.enums.TransactionEventService;
import com.dinhchieu.ewallet.transaction_service.enums.EventStatus;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionStatus;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionType;
import com.dinhchieu.ewallet.transaction_service.repositories.mongo.TransactionDocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionHistoryConsumer {
  private final TransactionDocumentRepository transactionDocumentRepository;

  @KafkaListener(topics = "transaction-events", groupId = "transaction-history-group")
  public void syncToMongo(ConsumerRecord<String, TransactionEvent> record) {
    TransactionEvent event = record.value();

    transactionDocumentRepository.findById(event.getSagaId()).ifPresent(doc -> {
      TransactionStatus newStatus = determinStatus(doc.getType(), event);
      doc.setStatus(newStatus);

      transactionDocumentRepository.save(doc);
      log.info("Updated TransactionDocument {} with new status: {}", doc.getId(), newStatus);
    });

  }

  private TransactionStatus determinStatus(TransactionType type, TransactionEvent event) {
    boolean isSuccess = event.getStatus().equals(EventStatus.SUCCESS.name());

    String eventService = event.getServiceName();

    if (!isSuccess) {
      if (type == TransactionType.DEPOSIT &&
          eventService.equals(TransactionEventService.BANK_ADAPTER_SERVICE.name())) {
        return TransactionStatus.FAILED;
      }

      if (type == TransactionType.DEPOSIT &&
          eventService.equals(TransactionEventService.WALLET_SERVICE.name())) {
        return TransactionStatus.COMPENSATED;
      }

      if (type == TransactionType.WITHDRAWAL &&
          eventService.equals(TransactionEventService.WALLET_SERVICE.name())) {
        return TransactionStatus.FAILED;
      }

      if (type == TransactionType.WITHDRAWAL &&
          eventService.equals(TransactionEventService.BANK_ADAPTER_SERVICE.name())) {
        return TransactionStatus.COMPENSATED;
      }

      if (type == TransactionType.TRANSFER &&
          eventService.equals(TransactionEventService.WALLET_SERVICE.name())) {
        return TransactionStatus.FAILED;
      }
    }

    if (type == TransactionType.DEPOSIT &&
        eventService.equals(TransactionEventService.WALLET_SERVICE.name())) {
      return TransactionStatus.COMPLETED;
    }

    if (type == TransactionType.WITHDRAWAL
        && eventService.equals(TransactionEventService.BANK_ADAPTER_SERVICE.name())) {
      return TransactionStatus.COMPLETED;
    }

    if (type == TransactionType.TRANSFER &&
        eventService.equals(TransactionEventService.WALLET_SERVICE.name())) {
      return TransactionStatus.COMPLETED;
    }

    return TransactionStatus.PENDING;
  }
}