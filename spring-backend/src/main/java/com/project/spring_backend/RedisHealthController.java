package com.project.spring_backend;

import java.util.Map;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisHealthController {

    private final RedisConnectionFactory redisConnectionFactory;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisHealthController(
            RedisConnectionFactory redisConnectionFactory,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping("/redis/health")
    public Map<String, String> redisHealth() {
        RedisConnection connection = redisConnectionFactory.getConnection();
        String pong = connection.ping();
        connection.close();

        return Map.of(
                "redis", "ok",
                "pong", pong
        );
    }

    @PostMapping("/cache/test")
    public Map<String, String> saveTestCache(@RequestParam String value) {
        stringRedisTemplate.opsForValue().set("test", value);

        return Map.of(
                "key", "test",
                "value", value
        );
    }

    @GetMapping("/cache/test")
    public Map<String, String> getTestCache() {
        String value = stringRedisTemplate.opsForValue().get("test");

        return Map.of(
                "key", "test",
                "value", value == null ? "" : value
        );
    }
}