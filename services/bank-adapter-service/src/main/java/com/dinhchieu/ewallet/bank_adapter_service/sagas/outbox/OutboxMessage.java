package com.dinhchieu.ewallet.bank_adapter_service.sagas.outbox;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.dinhchieu.ewallet.common_library.enums.OutboxStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "outbox_message", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status, created_at")
})
public class OutboxMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(name = "topic", nullable = false)
  private String topic;

  @Column(name = "saga_id")
  private String sagaId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(name = "retry_count")
  @Builder.Default
  private Integer retryCount = 0;

  @Column(name = "last_attempt")
  private LocalDateTime lastAttempt;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private OutboxStatus status = OutboxStatus.PENDING;

  @Column(name = "created_at", nullable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;
}
