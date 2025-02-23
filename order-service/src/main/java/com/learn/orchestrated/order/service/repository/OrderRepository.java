package com.learn.orchestrated.order.service.repository;

import com.learn.orchestrated.order.service.document.OrderDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

public interface OrderRepository extends MongoRepository<OrderDocument, String> {
}
