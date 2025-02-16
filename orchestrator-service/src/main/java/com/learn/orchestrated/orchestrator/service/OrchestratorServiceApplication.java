package com.learn.orchestrated.orchestrator.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.learn.orchestrated.orchestrator.service","com.learn.sagacommons"})
public class OrchestratorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrchestratorServiceApplication.class, args);
	}

}
