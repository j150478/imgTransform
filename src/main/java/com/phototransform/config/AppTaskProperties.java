package com.phototransform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用任务生命周期配置。
 *
 * <p>读取 {@code app.task.*} 配置项，用于控制任务超时判定、结果保留时长及清理调度策略。
 * {@link #timeoutHours} 控制 PROCESSING 状态任务超时阈值，
 * {@link #expiryHours} 控制已完成任务的可查询窗口，
 * {@link #cleanupCron} 控制定时清理的执行频率。</p>
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
