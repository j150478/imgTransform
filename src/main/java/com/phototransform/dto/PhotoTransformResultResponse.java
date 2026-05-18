package com.phototransform.dto;

import com.phototransform.enums.TransformStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 照片转化结果查询响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoTransformResultResponse {

    /** 任务当前处理状态 */
    private TransformStatus status;

    /** 生成的证件照图片URL，仅 SUCCESS 时有值 */
    private String resultImageUrl;

    /** 错误信息，仅 FAILED 时有值 */
    private String errorMessage;
}
