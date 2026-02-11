package com.phototransform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 图像生成请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationRequest {

    /** 生成提示词（必填） */
    private String prompt;

    /** 参考图像列表（URL或Base64），支持1-14张 */
    private List<String> referenceImages;

    /** 负面提示词 */
    private String negativePrompt;

    /** 图像尺寸，如: 1024x1024, 2048x2048 */
    private String size;

    /** 模型版本，如: doubao-seedream-4-5-251128 */
    private String model;

    /** 响应格式: url 或 b64_json */
    private String responseFormat;

    /** 是否开启水印 */
    private Boolean watermark;

    /** 组图生成模式: auto 或 disabled */
    private String sequentialImageGeneration;

    /** 生成图像数量（组图模式） */
    private Integer n;

    /** 任务超时时间（毫秒） */
    private Long timeout;

    /** 自定义扩展参数 */
    private Map<String, Object> extraParams;
}
