package com.learn.orchestrated.order.service.repository;

import com.learn.orchestrated.order.service.document.EventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends MongoRepository<EventDocument, String> {

    List<EventDocument> findAllByOrderByCreatedAtDesc();

    Optional<EventDocument> findTop1ByOrderIdOrderByCreatedAtDesc(String eventId);

    Optional<EventDocument> findTop1ByTransactionIdOrderByCreatedAtDesc(String TransactionId);
}
