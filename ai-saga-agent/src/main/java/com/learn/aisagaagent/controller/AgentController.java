package com.learn.aisagaagent.controller;

import com.learn.aisagaagent.service.DataAnalystAgentService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@AllArgsConstructor
public class AgentController {

    private final DataAnalystAgentService dataAnalystAgentService;

    @GetMapping("/chat")
    public String getChat(@RequestParam(name = "question") String question) {
        return dataAnalystAgentService.runAgent(question);
    }
}
