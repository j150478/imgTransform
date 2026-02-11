package com.phototransform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图像生成结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationResult {

    /** 任务ID */
    private String taskId;

    /** 状态: SUCCESS, PENDING, RUNNING, FAILED */
    private String status;

    /** 生成的图像列表 */
    private List<GeneratedImage> images;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 使用的模型 */
    private String model;

    /** 原始提示词 */
    private String prompt;

    /** 错误信息 */
    private String errorMessage;

    /** 错误码 */
    private String errorCode;

    /**
     * 生成的单张图像信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedImage {

        /** 图像索引 */
        private Integer index;

        /** 图像URL */
        private String url;

        /** Base64编码数据 */
        private String b64Json;

        /** 内容类型 */
        private String contentType;

        /** 宽度（像素） */
        private Integer width;

        /** 高度（像素） */
        private Integer height;

        /** 文件大小（字节） */
        private Long fileSize;

        /** 修订版本号 */
        private String revisedPrompt;
    }
}
