package com.learn.orchestrated.order.service.controller;

import com.learn.orchestrated.order.service.document.EventDocument;
import com.learn.orchestrated.order.service.dto.EventFilter;
import com.learn.orchestrated.order.service.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private EventDocument event;

    @BeforeEach
    void setUp() {
        event = new EventDocument();
        event.setEventId("evt123");
        event.setTransactionId("tx456");
        event.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void shouldReturnAllEvents() throws Exception {
        Mockito.when(eventService.findAll()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("evt123"))
                .andExpect(jsonPath("$[0].transactionId").value("tx456"));

        Mockito.verify(eventService).findAll();
    }

    @Test
    void shouldReturnEventByFilters() throws Exception {
        Mockito.when(eventService.findByFilters(any(EventFilter.class))).thenReturn(event);

        mockMvc.perform(get("/api/events/filters")
                        .param("orderId", "order-1")
                        .param("transactionId", "tx456")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt123"))
                .andExpect(jsonPath("$.transactionId").value("tx456"));

        Mockito.verify(eventService).findByFilters(any(EventFilter.class));
    }
}