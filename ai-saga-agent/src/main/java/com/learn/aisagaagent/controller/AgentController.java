package com.learn.aisagaagent.controller;

import com.learn.aisagaagent.model.SagaDiagnostic;
import com.learn.aisagaagent.repository.SagaDiagnosticRepository;
import com.learn.aisagaagent.service.DataAnalystAgentService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@AllArgsConstructor
public class AgentController {

    private final DataAnalystAgentService dataAnalystAgentService;
    private final SagaDiagnosticRepository diagnosticRepository;

    @GetMapping("/chat")
    public String getChat(@RequestParam(name = "question") String question) {
        return dataAnalystAgentService.runAgent(question);
    }

    @GetMapping("/diagnostics")
    public List<SagaDiagnostic> getAllDiagnostics() {
        return diagnosticRepository.findAllByOrderByCreatedAtDesc();
    }
}
