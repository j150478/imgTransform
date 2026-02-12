package com.phototransform.dto;

import com.phototransform.enums.TransformStatus;
import lombok.Data;

/**
 * 照片转化提交响应 DTO
 * 
 * 用于返回任务创建结果，包含任务ID和初始状态
 */
@Data
public class PhotoTransformResponse {

    /**
     * 任务唯一标识
     * 用于后续查询任务状态和结果
     */
    private String taskId;

    /**
     * 任务当前状态
     * 创建后初始状态通常为 PROCESSING
     * @see TransformStatus
     */
    private TransformStatus status;
}
