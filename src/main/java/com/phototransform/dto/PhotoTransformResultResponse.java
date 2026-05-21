package com.phototransform.dto;

import com.phototransform.enums.TransformStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 照片转化结果查询响应 DTO
 *
 * 客户端通过 taskId 查询转化结果时返回的响应。
 * 根据任务状态，不同的字段会有值：
 * - PROCESSING：仅返回 status
 * - SUCCESS：返回 status 和 resultImageUrl（结果图片可访问的 URL）
 * - FAILED：返回 status 和 errorMessage（错误原因说明）
 *
 * @see com.phototransform.enums.TransformStatus
 * @see PhotoTransformResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoTransformResultResponse {

    /**
     * 任务当前处理状态
     * PROCESSING：处理中，SUCCESS：成功，FAILED：失败
     */
    private TransformStatus status;

    /**
     * 生成的证件照图片 URL
     * 仅当 status = {@link TransformStatus#SUCCESS} 时有值，可公开访问
     */
    private String resultImageUrl;

    /**
     * 错误信息
     * 仅当 status = {@link TransformStatus#FAILED} 时有值，
     * 描述失败的具体原因，用于前端展示或排查问题
     */
    private String errorMessage;
}
