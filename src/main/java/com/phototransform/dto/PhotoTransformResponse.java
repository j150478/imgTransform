package com.phototransform.dto;

import com.phototransform.enums.TransformStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 照片转化提交响应 DTO
 *
 * 照片转化任务提交成功后，返回给客户端的响应结果。
 * 包含任务唯一标识和当前处理状态，客户端可据此轮询任务结果。
 *
 * @see PhotoTransformResultResponse
 * @see com.phototransform.enums.TransformStatus
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoTransformResponse {

    /**
     * 任务唯一标识
     * 由服务端生成的 UUID，用于后续查询转化结果
     */
    private String taskId;

    /**
     * 任务当前状态
     * 提交成功后通常为 {@link TransformStatus#PROCESSING}
     */
    private TransformStatus status;
}
