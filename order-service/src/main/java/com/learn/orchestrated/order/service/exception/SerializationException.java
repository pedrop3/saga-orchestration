package com.learn.orchestrated.order.service.exception;

public class SerializationException extends RuntimeException {

  public SerializationException(String message) {
    super(message);
  }
  public SerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
