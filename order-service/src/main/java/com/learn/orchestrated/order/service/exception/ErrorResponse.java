package com.learn.orchestrated.order.service.exception;

import java.util.List;

public record ErrorResponse(int status, String message,String exceptionName ,List<String> errors) {}

