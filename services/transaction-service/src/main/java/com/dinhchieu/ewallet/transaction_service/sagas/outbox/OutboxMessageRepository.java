package com.dinhchieu.ewallet.transaction_service.sagas.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, String> {
  @Query(value = "SELECT * FROM outbox_message WHERE status = 'PENDING' ORDER BY created_at ASC FOR UPDATE SKIP LOCKED LIMIT 100", nativeQuery = true)
  List<OutboxMessage> findTop100ByStatusOrderByCreatedAtAsc();
}
