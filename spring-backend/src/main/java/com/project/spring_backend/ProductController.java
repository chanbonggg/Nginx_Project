package com.project.spring_backend;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final JdbcTemplate jdbcTemplate;

    public ProductController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/products")
    public List<Map<String, Object>> getProducts() {
        String sql = "SELECT id, name, price FROM products ORDER BY id";

        return jdbcTemplate.queryForList(sql);
    }
}