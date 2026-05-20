package com.phototransform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用任务配置
 *
 * 读取 app.task.* 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.task")
public class AppTaskProperties {

    /** PROCESSING 任务最大存活时间（小时），超时标记 FAILED */
    private double timeoutHours = 1;

    /** 已完成任务保留时间（小时），过期后不可查询 */
    private int expiryHours = 24;

    /** 清理过期任务 cron 表达式 */
    private String cleanupCron = "0 0 * * * ?";
}
