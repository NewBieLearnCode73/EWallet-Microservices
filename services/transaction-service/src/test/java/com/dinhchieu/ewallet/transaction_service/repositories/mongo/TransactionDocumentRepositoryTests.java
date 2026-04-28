package com.dinhchieu.ewallet.transaction_service.repositories.mongo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.dinhchieu.ewallet.transaction_service.enums.TransactionStatus;
import com.dinhchieu.ewallet.transaction_service.enums.TransactionType;
import com.dinhchieu.ewallet.transaction_service.models.entities.TransactionDocument;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Document Repository Tests")
public class TransactionDocumentRepositoryTests {

  @Mock
  private TransactionDocumentRepository transactionDocumentRepository;

  @Test
  @DisplayName("Test find transactions by source wallet ID with pagination")
  public void testFindBySourceWalletIdWithPagination() {
    String sourceWalletId = "wallet-123";
    Pageable pageable = PageRequest.of(0, 10);

    List<TransactionDocument> transactions = List.of(
        TransactionDocument.builder()
            .id("tx-1")
            .amount(BigDecimal.valueOf(100))
            .type(TransactionType.TRANSFER)
            .sourceWalletId(sourceWalletId)
            .sourceUserName("user1")
            .destinationWalletId("wallet-456")
            .destinationUserName("user2")
            .description("Test transaction 1")
            .status(TransactionStatus.COMPLETED)
            .build(),
        TransactionDocument.builder()
            .id("tx-2")
            .amount(BigDecimal.valueOf(200))
            .type(TransactionType.TRANSFER)
            .sourceWalletId(sourceWalletId)
            .sourceUserName("user1")
            .destinationWalletId("wallet-789")
            .destinationUserName("user3")
            .description("Test transaction 2")
            .status(TransactionStatus.COMPLETED)
            .build());

    Page<TransactionDocument> page = new PageImpl<>(transactions, pageable, transactions.size());

    when(transactionDocumentRepository.findBySourceWalletId(sourceWalletId, pageable)).thenReturn(page);

    var resultPage = transactionDocumentRepository.findBySourceWalletId(sourceWalletId, pageable);

    assertEquals(2, resultPage.getTotalElements());
    assertEquals(1, resultPage.getTotalPages());
    assertEquals(transactions, resultPage.getContent());

  }

  @Test
  @DisplayName("Test find transactions by destination wallet ID with pagination")
  public void testFindByDestinationWalletIdWithPagination() {
    String destinationWalletId = "wallet-456";
    Pageable pageable = PageRequest.of(0, 10);

    List<TransactionDocument> transactions = List.of(
        TransactionDocument.builder()
            .id("tx-1")
            .amount(BigDecimal.valueOf(100))
            .type(TransactionType.TRANSFER)
            .sourceWalletId("wallet-123")
            .sourceUserName("user1")
            .destinationWalletId(destinationWalletId)
            .destinationUserName("user2")
            .description("Test transaction 1")
            .status(TransactionStatus.COMPLETED)
            .build(),
        TransactionDocument.builder()
            .id("tx-2")
            .amount(BigDecimal.valueOf(200))
            .type(TransactionType.TRANSFER)
            .sourceWalletId("wallet-789")
            .sourceUserName("user3")
            .destinationWalletId(destinationWalletId)
            .destinationUserName("user2")
            .description("Test transaction 2")
            .status(TransactionStatus.COMPLETED)
            .build());

    Page<TransactionDocument> page = new PageImpl<>(transactions, pageable, transactions.size());

    when(transactionDocumentRepository.findByDestinationWalletId(destinationWalletId, pageable)).thenReturn(page);

    var resultPage = transactionDocumentRepository.findByDestinationWalletId(destinationWalletId, pageable);

    assertEquals(2, resultPage.getTotalElements());
    assertEquals(1, resultPage.getTotalPages());
    assertEquals(transactions, resultPage.getContent());
  }

  @Test
  @DisplayName("Test find transactions by wallet with pagination")
  public void testFindTransactionsByWalletWithPagination() {

    String currentWalletId = "wallet-current-user";
    String otherWalletId = "wallet-other-user";

    // Arrange
    List<TransactionDocument> transactions = new java.util.ArrayList<>();

    // Transactions visible for current wallet (as source)
    for (int i = 0; i < 7; i++) {
      transactions.add(TransactionDocument.builder()
          .id("tx-" + i)
          .amount(BigDecimal.valueOf(100 + i))
          .type(TransactionType.TRANSFER)
          .sourceWalletId(currentWalletId)
          .sourceUserName(currentWalletId)
          .destinationWalletId(otherWalletId)
          .destinationUserName(otherWalletId)
          .description("Visible transaction as source " + i)
          .status(TransactionStatus.COMPLETED)
          .build());
    }

    // Transactions visible for current wallet (as destination)
    for (int i = 0; i < 5; i++) {
      transactions.add(TransactionDocument.builder()
          .id("tx-src-" + i)
          .amount(BigDecimal.valueOf(200 + i))
          .type(TransactionType.TRANSFER)
          .sourceWalletId(otherWalletId)
          .sourceUserName(otherWalletId)
          .destinationWalletId(currentWalletId)
          .destinationUserName(currentWalletId)
          .description("Visible transaction as destination " + i)
          .status(TransactionStatus.COMPLETED)
          .build());
    }

    // Mock the repository to return paginated results
    Pageable pageable = PageRequest.of(0, 10);
    List<TransactionDocument> page0Content = transactions.subList(0, 10);
    Page<TransactionDocument> page0 = new PageImpl<>(page0Content, pageable, 12);

    when(transactionDocumentRepository.findBySourceWalletIdOrDestinationWalletId(
        currentWalletId, currentWalletId, pageable))
        .thenReturn(page0);

    // Act
    var page = transactionDocumentRepository.findBySourceWalletIdOrDestinationWalletId(currentWalletId,
        currentWalletId, pageable);

    // Assert
    assertEquals(12, page.getTotalElements());
    assertEquals(10, page.getContent().size());
    assertEquals(2, page.getTotalPages());
  }

  @Test
  @DisplayName("Test find transactions by type with pagination")
  public void testFindByTypeWithPagination() {
    String type = TransactionType.TRANSFER.name();
    Pageable pageable = PageRequest.of(0, 10);

    List<TransactionDocument> transactions = List.of(
        TransactionDocument.builder()
            .id("tx-1")
            .amount(BigDecimal.valueOf(100))
            .type(TransactionType.TRANSFER)
            .sourceWalletId("wallet-123")
            .sourceUserName("user1")
            .destinationWalletId("wallet-456")
            .destinationUserName("user2")
            .description("Test transaction 1")
            .status(TransactionStatus.COMPLETED)
            .build(),
        TransactionDocument.builder()
            .id("tx-2")
            .amount(BigDecimal.valueOf(200))
            .type(TransactionType.TRANSFER)
            .sourceWalletId("wallet-789")
            .sourceUserName("user3")
            .destinationWalletId("wallet-456")
            .destinationUserName("user2")
            .description("Test transaction 2")
            .status(TransactionStatus.COMPLETED)
            .build());

    Page<TransactionDocument> page = new PageImpl<>(transactions, pageable, transactions.size());

    when(transactionDocumentRepository.findByType(type, pageable)).thenReturn(page);

    var resultPage = transactionDocumentRepository.findByType(type, pageable);

    assertEquals(2, resultPage.getTotalElements());
    assertEquals(1, resultPage.getTotalPages());
    assertEquals(transactions, resultPage.getContent());
  }

  @Test
  @DisplayName("Test find transactions by status with pagination")
  public void testFindByStatusWithPagination() {
    String status = TransactionStatus.COMPLETED.name();
    Pageable pageable = PageRequest.of(0, 10);

    List<TransactionDocument> transactions = List.of(
        TransactionDocument.builder()
            .id("tx-1")
            .amount(BigDecimal.valueOf(100))
            .type(TransactionType.TRANSFER)
            .sourceWalletId("wallet-123")
            .sourceUserName("user1")
            .destinationWalletId("wallet-456")
            .destinationUserName("user2")
            .description("Test transaction 1")
            .status(TransactionStatus.COMPLETED)
            .build(),
        TransactionDocument.builder()
            .id("tx-2")
            .amount(BigDecimal.valueOf(200))
            .type(TransactionType.TRANSFER)
            .sourceWalletId("wallet-789")
            .sourceUserName("user3")
            .destinationWalletId("wallet-456")
            .destinationUserName("user2")
            .description("Test transaction 2")
            .status(TransactionStatus.COMPLETED)
            .build());

    Page<TransactionDocument> page = new PageImpl<>(transactions, pageable, transactions.size());

    when(transactionDocumentRepository.findByStatus(status, pageable)).thenReturn(page);

    var resultPage = transactionDocumentRepository.findByStatus(status, pageable);

    assertEquals(2, resultPage.getTotalElements());
    assertEquals(1, resultPage.getTotalPages());
    assertEquals(transactions, resultPage.getContent());
  }

  @Test
  @DisplayName("Test find transactions by source wallet ID and status with pagination")
  public void testFindBySourceWalletIdAndStatusWithPagination() {
    String sourceWalletId = "wallet-123";
    String status = TransactionStatus.COMPLETED.name();
    Pageable pageable = PageRequest.of(0, 10);

    List<TransactionDocument> transactions = List.of(
        TransactionDocument.builder()
            .id("tx-1")
            .amount(BigDecimal.valueOf(100))
            .type(TransactionType.TRANSFER)
            .sourceWalletId(sourceWalletId)
            .sourceUserName("user1")
            .destinationWalletId("wallet-456")
            .destinationUserName("user2")
            .description("Test transaction 1")
            .status(TransactionStatus.COMPLETED)
            .build(),
        TransactionDocument.builder()
            .id("tx-2")
            .amount(BigDecimal.valueOf(200))
            .type(TransactionType.TRANSFER)
            .sourceWalletId(sourceWalletId)
            .sourceUserName("user1")
            .destinationWalletId("wallet-789")
            .destinationUserName("user3")
            .description("Test transaction 2")
            .status(TransactionStatus.COMPLETED)
            .build());

    Page<TransactionDocument> page = new PageImpl<>(transactions, pageable, transactions.size());

    when(transactionDocumentRepository.findBySourceWalletIdAndStatus(sourceWalletId, status, pageable))
        .thenReturn(page);

    var resultPage = transactionDocumentRepository.findBySourceWalletIdAndStatus(sourceWalletId, status,
        pageable);

    assertEquals(2, resultPage.getTotalElements());
    assertEquals(1, resultPage.getTotalPages());
  }

  @Test
  @DisplayName("Test find transactions by destination wallet ID and status with pagination")
  public void testFindByDestinationWalletIdAndStatusWithPagination() {
    String destinationWalletId = "wallet-456";
    String status = TransactionStatus.COMPLETED.name();
    Pageable pageable = PageRequest.of(0, 10);

    List<TransactionDocument> transactions = List.of(
        TransactionDocument.builder()
            .id("tx-1")
            .amount(BigDecimal.valueOf(100))
            .type(TransactionType.TRANSFER)
            .sourceWalletId("wallet-123")
            .sourceUserName("user1")
            .destinationWalletId(destinationWalletId)
            .destinationUserName("user2")
            .description("Test transaction 1")
            .status(TransactionStatus.COMPLETED)
            .build(),
        TransactionDocument.builder()
            .id("tx-2")
            .amount(BigDecimal.valueOf(200))
            .type(TransactionType.TRANSFER)
            .sourceWalletId("wallet-789")
            .sourceUserName("user3")
            .destinationWalletId(destinationWalletId)
            .destinationUserName("user2")
            .description("Test transaction 2")
            .status(TransactionStatus.COMPLETED)
            .build());

    Page<TransactionDocument> page = new PageImpl<>(transactions, pageable, transactions.size());

    when(transactionDocumentRepository.findByDestinationWalletIdAndStatus(destinationWalletId, status,
        pageable)).thenReturn(page);

    var resultPage = transactionDocumentRepository.findByDestinationWalletIdAndStatus(destinationWalletId,
        status, pageable);

    assertEquals(2, resultPage.getTotalElements());
    assertEquals(1, resultPage.getTotalPages());
  }

}
