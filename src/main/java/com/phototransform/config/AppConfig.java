package com.phototransform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.web.client.RestTemplate;

import java.nio.file.Paths;
import java.util.concurrent.Executor;

/**
 * 应用配置类
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig implements WebMvcConfigurer {

    @Autowired
    private AppStorageProperties storageProperties;

    /**
     * 异步任务处理线程池
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("transform-");
        executor.setRejectedExecutionHandler((r, e) -> {
            throw new RuntimeException("任务队列已满，请稍后重试");
        });
        executor.initialize();
        log.info("异步任务线程池初始化完成");
        return executor;
    }

    /**
     * RestTemplate Bean（用于 Supabase Storage REST API）
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 静态资源映射：将上传目录映射为可访问的 URL（仅 local 存储类型）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!"local".equals(storageProperties.getType())) {
            log.info("存储类型为 {}，跳过本地静态资源映射", storageProperties.getType());
            return;
        }
        String absolutePath = Paths.get(storageProperties.getLocalPath()).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/**", "/dev-uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");
        log.info("静态资源映射: /uploads/** -> file:{}", absolutePath);
    }

    /**
     * CORS 跨域配置
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
