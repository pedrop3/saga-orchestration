package com.learn.orchestrated.product.validation.service.repository;

import com.learn.orchestrated.product.validation.service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductValidationRepository extends JpaRepository<Product, Integer> {

    boolean existsByCode(String code);
}
