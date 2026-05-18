package com.phototransform.service.impl;

import com.phototransform.common.TaskCreatedEvent;
import com.phototransform.domain.entity.PhotoTransformTask;
import com.phototransform.service.PhotoTransformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步任务执行器
 *
 * 监听 TaskCreatedEvent，异步执行任务处理，与 ServiceImpl 无循环依赖
 */
@Slf4j
@Component
public class AsyncTaskExecutor {

    @Autowired
    private PhotoTransformService photoTransformService;

    /**
     * 异步处理任务创建事件
     *
     * @param event 任务创建事件
     */
    @Async("taskExecutor")
    @EventListener
    public void onTaskCreated(TaskCreatedEvent event) {
        PhotoTransformTask task = event.getTask();
        log.info("[{}] 异步任务开始执行", task.getTaskId());
        photoTransformService.processTransformTask(task);
    }
}
