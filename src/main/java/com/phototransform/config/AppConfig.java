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
 * 应用核心配置类。
 *
 * <p>配置异步任务线程池、RestTemplate、静态资源映射及 CORS 跨域策略。
 * 通过 {@link EnableAsync} 和 {@link EnableScheduling} 启用异步任务与定时调度能力。</p>
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig implements WebMvcConfigurer {

    @Autowired
    private AppStorageProperties storageProperties;

    /**
     * 异步任务处理线程池。
     *
     * <p>核心线程数 4，最大线程数 8，队列容量 100。
     * 拒绝策略：队列满时抛出 {@link RuntimeException}，提示调用方稍后重试。</p>
     *
     * @return 异步任务执行器
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
     * 提供 {@link RestTemplate} 实例，用于调用 Supabase Storage REST API。
     *
     * @return RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 配置静态资源映射，将本地上传目录暴露为可访问的 URL。
     *
     * <p>仅当存储类型为 {@code local} 时生效，将 {@code /uploads/**} 和 {@code /dev-uploads/**}
     * 路径映射到 {@link AppStorageProperties#getLocalPath()} 指定的文件系统目录。</p>
     *
     * @param registry 资源处理器注册表
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
     * 配置 CORS 跨域过滤器。
     *
     * <p>允许所有来源、方法、请求头，不携带凭据，预检请求有效期 3600 秒。
     * 以最高优先级注册，确保跨域请求在所有其他过滤器之前被处理。</p>
     *
     * @return {@link CorsFilter} 的过滤器注册 Bean
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
