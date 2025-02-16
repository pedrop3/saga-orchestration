package com.learn.orchestrated.product.validation.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.learn.orchestrated.product.validation.service", "com.learn.sagacommons"})
public class ProductValidationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductValidationServiceApplication.class, args);
	}

}
