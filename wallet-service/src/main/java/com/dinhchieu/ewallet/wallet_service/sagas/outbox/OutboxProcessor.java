package com.dinhchieu.ewallet.wallet_service.sagas.outbox;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dinhchieu.ewallet.avro.TransactionEvent;
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

      TransactionEvent transactionEvent = objectMapper.readValue(message.getPayload(), TransactionEvent.class);

      kafkaTemplate.send(message.getTopic(), message.getSagaId(), transactionEvent).get(3, TimeUnit.SECONDS);

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
