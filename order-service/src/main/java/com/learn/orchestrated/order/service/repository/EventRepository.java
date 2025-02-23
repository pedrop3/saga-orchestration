package com.learn.orchestrated.order.service.repository;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.sagacommons.dto.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends MongoRepository<EventDocument, Long> {
}
