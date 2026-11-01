package com.ecommerce.backend.repository;

import com.ecommerce.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Spring generates SQL from the method name automatically:
    List<Product> findByCategory(String category);
    List<Product> findByNameContainingIgnoreCase(String keyword);
    List<Product> findByStockGreaterThan(Integer threshold);

    // Custom JPQL query when method name isn't enough:
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :min AND :max")
    List<Product> findByPriceRange(@Param("min") BigDecimal min,
                                   @Param("max") BigDecimal max);
}