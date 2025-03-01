package com.learn.orchestrated.order.service.exception;

public class InvalidArgumentsException extends RuntimeException {
  public InvalidArgumentsException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidArgumentsException(String message) {
    super(message);
  }
}
