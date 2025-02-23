package com.learn.orchestrated.order.service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.document.OrderDocument;
import com.learn.orchestrated.order.service.dto.OrderRequestDTO;
import com.learn.orchestrated.order.service.exception.OrderProcessingException;
import com.learn.orchestrated.order.service.exception.SerializationException;
import com.learn.orchestrated.order.service.producer.SagaProducer;
import com.learn.orchestrated.order.service.repository.EventRepository;
import com.learn.orchestrated.order.service.repository.OrderRepository;
import com.learn.orchestrated.order.service.service.EventPublisherService;
import com.learn.orchestrated.order.service.service.EventService;
import com.learn.orchestrated.order.service.service.OrderService;
import com.learn.sagacommons.dto.Event;
import com.learn.sagacommons.dto.Order;
import com.learn.sagacommons.utils.JsonUtil;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.Buffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final String TRANSACTION_ID_PATTERN = "%s_%s";

    private final OrderRepository orderRepository;
    private final EventPublisherService eventPublisherService;
    private final JsonUtil jsonUtil;


    @Transactional
    public OrderDocument createOrder(OrderRequestDTO orderRequestDTO) {
        try {
            logger.info("Iniciando criação da ordem...");

            var orderDocument = saveOrder(orderRequestDTO);
            var eventDocument = createEventPayload(orderDocument);

            eventPublisherService.publish(eventDocument);

            logger.info("Ordem {} criada com sucesso.", orderDocument.getOrderId());
            return orderDocument;
        } catch (DataAccessException e) {
            logger.error("Erro ao acessar o banco de dados.", e);
            throw new OrderProcessingException("Erro ao salvar ordem no banco de dados.", e);
        } catch (Exception e) {
            logger.error("Erro inesperado ao processar ordem.", e);
            throw new OrderProcessingException("Erro ao processar evento da ordem.", e);
        }
    }

    private OrderDocument saveOrder(OrderRequestDTO orderRequestDTO) {
        OrderDocument orderDocument = new OrderDocument(
                orderRequestDTO.getProducts(),
                LocalDateTime.now(),
                generateTransactionId()
        );
        return orderRepository.save(orderDocument);
    }

    private String generateTransactionId() {
        return String.format("%s_%d", UUID.randomUUID(), Instant.now().toEpochMilli());
    }

    private EventDocument createEventPayload(OrderDocument orderDocument) {
        return new EventDocument(orderDocument.getOrderId(), orderDocument.getTransactionId(),
                orderDocument, LocalDateTime.now());
    }

}
