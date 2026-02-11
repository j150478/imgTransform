package com.phototransform.domain.entity;

import com.phototransform.domain.enums.ModelType;
import com.phototransform.domain.enums.TransformStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 证件照转化任务实体
 * 
 * 描述一次证件照转化任务的完整信息，作为领域模型存在
 */
@Data
public class PhotoTransformTask {

    /**
     * 任务唯一标识
     */
    private String taskId;

    /**
     * 原始图片URL
     * 用户上传的原始照片存储地址
     */
    private String originalImageUrl;

    /**
     * 处理结果图片URL
     * 证件照生成后的图片存储地址
     */
    private String resultImageUrl;

    /**
     * 任务处理状态
     * @see TransformStatus
     */
    private TransformStatus status;

    /**
     * 使用的图像处理模型类型
     * @see ModelType
     */
    private ModelType modelType;

    /**
     * 证件照背景颜色
     * 如：white、blue、red
     */
    private String backgroundColor;

    /**
     * 证件照类型
     * 如：id_card、passport、driver_license
     */
    private String photoType;

    /**
     * 错误信息
     * 当处理失败时记录错误原因
     */
    private String errorMessage;

    /**
     * 任务创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 任务更新时间
     */
    private LocalDateTime updatedTime;
}
