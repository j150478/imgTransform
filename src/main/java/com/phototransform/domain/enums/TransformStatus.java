package com.phototransform.domain.enums;

/**
 * 证件照转化任务状态枚举
 * 
 * 描述任务在生命周期中的各种状态
 */
public enum TransformStatus {
    
    /**
     * 处理中 - 任务已创建，正在进行图像处理
     */
    PROCESSING,
    
    /**
     * 成功 - 图像处理完成，结果可用
     */
    SUCCESS,
    
    /**
     * 失败 - 图像处理过程中发生错误
     */
    FAILED
}
