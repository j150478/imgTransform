package com.phototransform.dto;

import com.phototransform.enums.TransformStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 照片转化提交响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoTransformResponse {

    /** 任务唯一标识 */
    private String taskId;

    /** 任务当前状态 */
    private TransformStatus status;
}
