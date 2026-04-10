package com.dinhchieu.ewallet.transaction_service.repositories.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.dinhchieu.ewallet.transaction_service.models.entities.TransactionDocument;

@Repository
public interface TransactionDocumentRepository extends MongoRepository<TransactionDocument, String> {
  Page<TransactionDocument> findAll(Pageable pageable);

  /**
   * Find transactions by source wallet ID with pagination
   * 
   * @param sourceWalletId the source wallet ID to search for
   * @param pageable       pagination information
   * @return page of transactions
   */
  Page<TransactionDocument> findBySourceWalletId(String sourceWalletId, Pageable pageable);

  /**
   * Find transactions by destination wallet ID with pagination
   * 
   * @param destinationWalletId the destination wallet ID to search for
   * @param pageable            pagination information
   * @return page of transactions
   */
  Page<TransactionDocument> findByDestinationWalletId(String destinationWalletId, Pageable pageable);

  /**
   * Find transactions by source or destination wallet ID with pagination
   * 
   * @param sourceWalletId      the source wallet ID to search for
   * @param destinationWalletId the destination wallet ID to search for
   * @param pageable            pagination information
   * @return page of transactions
   */
  Page<TransactionDocument> findBySourceWalletIdOrDestinationWalletId(String sourceWalletId, String destinationWalletId,
      Pageable pageable);

  /**
   * Find transactions by transaction type with pagination
   * 
   * @param type     the transaction type to search for
   * @param pageable pagination information
   * @return page of transactions
   */
  @Query("{ 'type': ?0 }")
  Page<TransactionDocument> findByType(String type, Pageable pageable);

  /**
   * Find transactions by status with pagination
   * 
   * @param status   the transaction status to search for
   * @param pageable pagination information
   * @return page of transactions
   */
  Page<TransactionDocument> findByStatus(String status, Pageable pageable);

  /**
   * Find transactions by source wallet ID and status with pagination
   * 
   * @param sourceWalletId the source wallet ID to search for
   * @param status         the transaction status to search for
   * @param pageable       pagination information
   * @return page of transactions
   */
  Page<TransactionDocument> findBySourceWalletIdAndStatus(String sourceWalletId, String status, Pageable pageable);

  /**
   * Find transactions by destination wallet ID and status with pagination
   * 
   * @param destinationWalletId the destination wallet ID to search for
   * @param status              the transaction status to search for
   * @param pageable            pagination information
   * @return page of transactions
   */
  Page<TransactionDocument> findByDestinationWalletIdAndStatus(String destinationWalletId, String status,
      Pageable pageable);
}
