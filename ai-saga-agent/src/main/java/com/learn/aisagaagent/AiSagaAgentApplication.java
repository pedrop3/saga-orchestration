package com.learn.aisagaagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan
public class AiSagaAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiSagaAgentApplication.class, args);
	}

}
