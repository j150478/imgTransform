package com.phototransform.common;

import org.springframework.context.ApplicationEvent;

/**
 * 任务创建事件
 *
 * ServiceImpl 发布事件，监听器异步处理，解耦循环依赖。
 * 事件仅携带 taskId，避免 JPA 实体泄漏到事件/监听器层。
 */
public class TaskCreatedEvent extends ApplicationEvent {

    private final String taskId;

    /**
     * 构造任务创建事件
     *
     * @param source 事件源
     * @param taskId 任务唯一标识
     */
    public TaskCreatedEvent(Object source, String taskId) {
        super(source);
        this.taskId = taskId;
    }

    /**
     * 获取任务ID
     *
     * @return 任务唯一标识
     */
    public String getTaskId() {
        return taskId;
    }
}
