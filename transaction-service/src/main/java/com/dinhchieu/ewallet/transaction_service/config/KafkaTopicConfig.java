package com.dinhchieu.ewallet.transaction_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
  @Bean
  public NewTopic bankCommandsTopic() {
    return TopicBuilder.name("bank-commands")
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic walletCommandsTopic() {
    return TopicBuilder.name("wallet-commands")
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic transactionEventsTopic() {
    return TopicBuilder.name("transaction-events")
        .partitions(3)
        .replicas(1)
        .build();
  }
}
