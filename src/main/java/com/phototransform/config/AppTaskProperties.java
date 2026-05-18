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

    /** 任务过期时间（小时） */
    private int expireHours = 24;

    /** 清理过期任务 cron 表达式 */
    private String cleanupCron = "0 0 2 * * ?";
}
