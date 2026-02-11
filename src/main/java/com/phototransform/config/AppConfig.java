package com.phototransform.config;

import org.springframework.context.annotation.Configuration;

/**
 * 应用配置类
 * 
 * 用于配置应用级别的通用设置
 * TODO: 后续可在此配置线程池、缓存、跨域等
 */
@Configuration
public class AppConfig {

    // TODO: 配置任务处理线程池
    // 用于异步执行图像处理任务
    // 建议配置：
    // - 核心线程数：根据 CPU 核心数配置
    // - 最大线程数：根据内存和处理能力配置
    // - 队列容量：根据预期并发量配置

    // TODO: 配置对象存储客户端
    // 用于上传和下载图片
    // 如阿里云 OSS、腾讯云 COS、AWS S3 等

    // TODO: 配置缓存
    // 用于缓存任务状态，减少数据库查询
    // 如 Redis、Caffeine 等

    // TODO: 配置跨域支持
    // 如果前端应用部署在不同域名下，需要配置 CORS
}
