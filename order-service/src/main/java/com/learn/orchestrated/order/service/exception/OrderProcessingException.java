package com.learn.orchestrated.order.service.exception;

public class OrderProcessingException extends RuntimeException {
  public OrderProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
