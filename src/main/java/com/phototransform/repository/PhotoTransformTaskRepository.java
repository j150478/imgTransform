package com.phototransform.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.phototransform.domain.entity.PhotoTransformTask;
import com.phototransform.enums.TransformStatus;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 证件照转化任务数据访问接口
 *
 * 基于 Spring Data JPA，自动派生查询方法。
 */
public interface PhotoTransformTaskRepository extends JpaRepository<PhotoTransformTask, String> {

    /**
     * 查询指定状态且创建时间早于阈值的任务（用于超时清理）
     *
     * @param status 任务状态
     * @param before 时间阈值
     * @return 符合条件的任务列表
     */
    List<PhotoTransformTask> findByStatusAndCreatedTimeBefore(TransformStatus status, LocalDateTime before);
}
