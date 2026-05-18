package com.phototransform.repository.impl;

import com.phototransform.domain.entity.PhotoTransformTask;
import com.phototransform.repository.PhotoTransformTaskRepository;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存任务仓库实现
 *
 * 基于 ConcurrentHashMap 的内存存储，用于开发阶段快速验证
 * 后续替换为 MyBatis 实现
 */
@Repository
public class InMemoryTaskRepository implements PhotoTransformTaskRepository {

    private final ConcurrentHashMap<String, PhotoTransformTask> store = new ConcurrentHashMap<>();

    @Override
    public PhotoTransformTask save(PhotoTransformTask task) {
        store.put(task.getTaskId(), task);
        return task;
    }

    @Override
    public PhotoTransformTask findByTaskId(String taskId) {
        return store.get(taskId);
    }
}
