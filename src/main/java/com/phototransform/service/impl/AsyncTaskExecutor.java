package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.common.TaskCreatedEvent;
import com.phototransform.domain.entity.PhotoTransformTask;
import com.phototransform.repository.PhotoTransformTaskRepository;
import com.phototransform.service.PhotoTransformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步任务执行器
 *
 * 监听 TaskCreatedEvent，根据 taskId 从数据库加载任务实体后异步执行处理，
 * 避免事件层泄漏 JPA 实体
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskExecutor {

    private final PhotoTransformService photoTransformService;
    private final PhotoTransformTaskRepository taskRepository;

    /**
     * 异步处理任务创建事件
     *
     * 1. 从事件中获取 taskId
     * 2. 从数据库加载任务实体
     * 3. 调用服务处理任务
     *
     * @param event 任务创建事件
     */
    @Async("taskExecutor")
    @EventListener
    public void onTaskCreated(TaskCreatedEvent event) {
        // 1. 获取 taskId
        String taskId = event.getTaskId();
        log.info("[{}] 异步任务开始执行", taskId);

        // 2. 从数据库加载任务实体
        PhotoTransformTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "异步处理时任务不存在: " + taskId));

        // 3. 调用服务处理任务
        photoTransformService.processTransformTask(task);
    }
}
