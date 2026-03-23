package com.dinhchieu.ewallet.transaction_service.sagas.outbox;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxScheduler {
  private final OutboxMessageRepository outboxMessageRepository;
  private final OutboxProcessor outboxProcessor;

  @Scheduled(fixedDelay = 2000)
  public void processOutboxMessages() {
    List<OutboxMessage> messages = outboxMessageRepository.findTop100ByStatusOrderByCreatedAtAsc();

    if (messages.isEmpty()) {
      log.info("No pending outbox messages found.");
      return;
    }

    for (OutboxMessage message : messages) {
      try {
        outboxProcessor.processSingleMessage(message);
      } catch (Exception e) {
        log.error("Error processing outbox message ID: {}", message.getId(), e);
        break;
      }
    }
  }

}
