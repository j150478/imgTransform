package com.phototransform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类。
 *
 * <p>配置 {@link RedisTemplate} 的键值序列化方式为字符串，
 * 用于 Token 黑名单等字符串键值对操作。</p>
 */
@Configuration
public class RedisConfig {

    /**
     * 配置字符串类型 RedisTemplate。
     *
     * <p>键和值均使用 {@link StringRedisSerializer} 序列化，
     * 确保可读性和跨语言兼容性。</p>
     *
     * @param factory Redis 连接工厂（由 Spring Boot 自动配置）
     * @return 字符串序列化的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
