package com.dinhchieu.ewallet.transaction_service.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.dinhchieu.ewallet.transaction_service.enums.TransactionStatus;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transaction_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDocument {
  @Id
  private String id;
  private BigDecimal amount;
  private TransactionType type;

  private String sourceWalletId;
  private String sourceUserName;

  private String destinationWalletId;
  private String destinationUserName;

  private String description;
  private TransactionStatus status;

  @CreatedDate
  @Field("created_at")
  private LocalDateTime createdAt;
  
  @LastModifiedDate
  @Field("updated_at")
  private LocalDateTime updatedAt;
}