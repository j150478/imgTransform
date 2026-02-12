package com.phototransform.dto;

import com.phototransform.enums.TransformStatus;
import lombok.Data;

/**
 * 照片转化结果查询响应 DTO
 * 
 * 用于返回任务处理状态和结果信息
 */
@Data
public class PhotoTransformResultResponse {

    /**
     * 任务当前处理状态
     * PROCESSING - 处理中
     * SUCCESS - 处理成功
     * FAILED - 处理失败
     * @see TransformStatus
     */
    private TransformStatus status;

    /**
     * 生成的证件照图片URL
     * 仅当 status 为 SUCCESS 时有值
     */
    private String resultImageUrl;

    /**
     * 错误信息
     * 仅当 status 为 FAILED 时有值
     */
    private String errorMessage;
}
