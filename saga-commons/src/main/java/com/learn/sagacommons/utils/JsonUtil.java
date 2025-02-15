package com.learn.sagacommons.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.sagacommons.dto.Event;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
@RequiredArgsConstructor
public final class JsonUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtil.class);
    private final ObjectMapper objectMapper;

    public Optional<String> toJson(Object object) {
        try {
            return Optional.ofNullable(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException ex) {
            LOGGER.error("Error converting object to JSON: {}", object, ex);
            return Optional.empty();
        }
    }

    public Optional<Event> toEvent(String json) {
        try {
            return Optional.ofNullable(objectMapper.readValue(json, Event.class));
        } catch (JsonProcessingException ex) {
            LOGGER.error("Error converting JSON to Event: {}", json, ex);
            return Optional.empty();
        }
    }
}