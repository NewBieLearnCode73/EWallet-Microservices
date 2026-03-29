package com.dinhchieu.ewallet.transaction_service.sagas.outbox;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dinhchieu.ewallet.avro.BankCommand;
import com.dinhchieu.ewallet.avro.TransactionEvent;
import com.dinhchieu.ewallet.avro.WalletCommand;
import com.dinhchieu.ewallet.common_library.enums.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
  private final OutboxMessageRepository outboxMessageRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processSingleMessage(OutboxMessage message) {
    try {
      if (message.getRetryCount() >= 5) {
        log.warn("Message ID: {} max retries reached.", message.getId());
        message.setStatus(OutboxStatus.FAILED);
        outboxMessageRepository.save(message);
        return;
      }

      Object payloadObject = null;

      if ("bank-commands".equals(message.getTopic())) {
        payloadObject = objectMapper.readValue(message.getPayload(), BankCommand.class);
      } else if ("wallet-commands".equals(message.getTopic())) {
        payloadObject = objectMapper.readValue(message.getPayload(), WalletCommand.class);
      } else if ("transaction-events".equals(message.getTopic())) {
        payloadObject = objectMapper.readValue(message.getPayload(), TransactionEvent.class);
      } else {
        throw new RuntimeException("Unknown topic: " + message.getTopic());
      }
      // Sync send with timeout to ensure we know if it succeeded or failed
      kafkaTemplate.send(message.getTopic(), message.getSagaId(), payloadObject).get(3, TimeUnit.SECONDS);

      message.setStatus(OutboxStatus.SENT);
      message.setRetryCount(0);
      outboxMessageRepository.save(message);

    } catch (Exception e) {
      log.error("Failed to send message ID: {}", message.getId(), e);
      message.setRetryCount(message.getRetryCount() + 1);
      message.setLastAttempt(LocalDateTime.now());
      outboxMessageRepository.save(message);

      throw new RuntimeException("Kafka send failed", e);
    }
  }
}
