package com.phototransform.repository;

import com.phototransform.domain.entity.PhotoTransformTask;

/**
 * 证件照转化任务数据访问接口
 * 
 * 负责任务实体的持久化操作，包括保存、查询等功能。
 * 当前为占位接口，具体实现可选择使用 JPA、MyBatis 或其他持久化框架。
 */
public interface PhotoTransformTaskRepository {

    /**
     * 保存任务记录
     * 
     * 将任务实体持久化到存储介质（如数据库、缓存等）。
     * 如果任务已存在则更新，不存在则创建新记录。
     * 
     * TODO: 实现具体的持久化逻辑
     * - 使用 Spring Data JPA 时继承 JpaRepository
     * - 使用 MyBatis 时编写对应的 XML Mapper
     * - 或使用 Redis 等缓存存储
     * 
     * @param task 待保存的任务实体
     * @return 保存后的任务实体（包含生成的ID等）
     */
    PhotoTransformTask save(PhotoTransformTask task);

    /**
     * 根据任务ID查询任务
     * 
     * 通过任务唯一标识查询对应的任务实体。
     * 
     * TODO: 实现具体的查询逻辑
     * - 实现数据库查询语句
     * - 考虑添加缓存层提升查询性能
     * 
     * @param taskId 任务唯一标识
     * @return 查询到的任务实体，如果不存在返回 null
     */
    PhotoTransformTask findByTaskId(String taskId);
}
