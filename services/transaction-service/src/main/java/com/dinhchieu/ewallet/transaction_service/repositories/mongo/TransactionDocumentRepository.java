package com.dinhchieu.ewallet.transaction_service.repositories.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dinhchieu.ewallet.transaction_service.models.entities.TransactionDocument;

@Repository
public interface TransactionDocumentRepository extends MongoRepository<TransactionDocument, String> {
}
