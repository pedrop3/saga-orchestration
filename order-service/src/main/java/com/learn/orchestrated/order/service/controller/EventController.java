package com.learn.orchestrated.order.service.controller;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.dto.EventFilter;
import com.learn.orchestrated.order.service.service.EventService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @GetMapping("/filters")
    public ResponseEntity<EventDocument> findByFilters(@RequestBody @Valid EventFilter eventFilter) {
        EventDocument eventDocument = eventService.findByFilters(eventFilter);
        return ResponseEntity.status(HttpStatus.OK).body(eventDocument);
    }

    @GetMapping
    public ResponseEntity<List<EventDocument>> findAll() {
        List<EventDocument> eventDocument = eventService.findAll();
        return ResponseEntity.status(HttpStatus.OK).body(eventDocument);
    }
}
