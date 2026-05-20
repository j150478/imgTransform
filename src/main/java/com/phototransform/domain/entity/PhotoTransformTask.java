package com.phototransform.domain.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.phototransform.enums.ModelType;
import com.phototransform.enums.TransformStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 证件照转化任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class PhotoTransformTask {

    /**
     * 任务唯一标识（主键）
     */
    @Id
    private String taskId;

    /**
     * 原始图片URL
     */
    @Column
    private String originalImageUrl;

    /**
     * 处理结果图片URL
     */
    @Column
    private String resultImageUrl;

    /**
     * 任务处理状态
     * @see TransformStatus
     */
    @Enumerated(EnumType.STRING)
    @Column
    private TransformStatus status;

    /**
     * 使用的图像处理模型类型
     * @see ModelType
     */
    @Enumerated(EnumType.STRING)
    @Column
    private ModelType modelType;

    /**
     * 证件照背景颜色
     */
    @Column
    private String backgroundColor;

    /**
     * 证件照类型
     */
    @Column
    private String photoType;

    /**
     * 错误信息
     */
    @Column
    private String errorMessage;

    /**
     * 任务创建时间
     */
    @Column
    private LocalDateTime createdTime;

    /**
     * 任务更新时间
     */
    @Column
    private LocalDateTime updatedTime;
}
