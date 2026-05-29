package com.phototransform.common;

import org.springframework.context.ApplicationEvent;

/**
 * 图像生成任务创建事件，仅携带 taskId
 */
public class ImageTaskCreatedEvent extends ApplicationEvent {

    private final String taskId;

    public ImageTaskCreatedEvent(Object source, String taskId) {
        super(source);
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}
