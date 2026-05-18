package com.phototransform.common;

import com.phototransform.domain.entity.PhotoTransformTask;
import org.springframework.context.ApplicationEvent;

/**
 * 任务创建事件
 *
 * ServiceImpl 发布事件，监听器异步处理，解耦循环依赖
 */
public class TaskCreatedEvent extends ApplicationEvent {

    private final PhotoTransformTask task;

    public TaskCreatedEvent(Object source, PhotoTransformTask task) {
        super(source);
        this.task = task;
    }

    public PhotoTransformTask getTask() {
        return task;
    }
}
