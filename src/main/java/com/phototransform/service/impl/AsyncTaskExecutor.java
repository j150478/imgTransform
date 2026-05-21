package com.phototransform.service.impl;

import com.phototransform.common.TaskCreatedEvent;
import com.phototransform.service.PhotoTransformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步任务执行器
 *
 * 监听 TaskCreatedEvent，根据 taskId 委托 PhotoTransformService 异步执行处理，
 * 不感知持久化层
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskExecutor {

    private final PhotoTransformService photoTransformService;

    /**
     * 异步处理任务创建事件
     *
     * 1. 从事件中获取 taskId
     * 2. 委托服务处理任务
     *
     * @param event 任务创建事件
     */
    @Async("taskExecutor")
    @EventListener
    public void onTaskCreated(TaskCreatedEvent event) {
        // 1. 获取 taskId
        String taskId = event.getTaskId();
        log.info("[{}] 异步任务开始执行", taskId);

        // 2. 委托服务处理任务（由服务内部加载实体）
        photoTransformService.processTransformTask(taskId);
    }
}
