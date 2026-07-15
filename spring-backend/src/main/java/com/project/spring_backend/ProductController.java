package com.project.spring_backend;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private static final String PRODUCTS_CACHE_KEY = "products:all";

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public ProductController(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/products")
    public Map<String, Object> getProducts() throws Exception {
        String cachedProducts = stringRedisTemplate.opsForValue().get(PRODUCTS_CACHE_KEY);

        if (cachedProducts != null) {
            List<Map<String, Object>> products = objectMapper.readValue(
                    cachedProducts,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            return Map.of(
                    "source", "redis",
                    "items", products
            );
        }

        String sql = "SELECT id, name, price FROM products ORDER BY id";
        List<Map<String, Object>> products = jdbcTemplate.queryForList(sql);

        String productsJson = objectMapper.writeValueAsString(products);
        stringRedisTemplate.opsForValue().set(PRODUCTS_CACHE_KEY, productsJson);

        return Map.of(
                "source", "postgres",
                "items", products
        );
    }
}