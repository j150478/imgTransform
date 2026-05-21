package com.phototransform.dto;

import com.phototransform.enums.ModelType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

/**
 * 照片转化请求 DTO
 *
 * 封装前端提交的证件照转化请求参数，使用 multipart/form-data 格式上传。
 *
 * @see com.phototransform.controller.PhotoTransformController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoTransformRequest {

    /**
     * 用户上传的原始照片文件
     * 支持常见图片格式（JPEG、PNG 等），建议文件大小不超过 10MB
     */
    @NotNull(message = "上传文件不能为空")
    private MultipartFile file;

    /**
     * 图像处理模型类型
     * 指定使用的 AI 模型，如 {@link ModelType#SEEDREAM_45}
     * 可选，默认由服务端决定
     */
    private ModelType modelType;

    /**
     * 证件照背景颜色代码
     * 1 = 蓝色，2 = 红色，3 = 白色
     *
     * @see com.phototransform.enums.BackgroundColor#fromCode(int)
     */
    private Integer backgroundColor;

    /**
     * 证件照类型
     * 用于选择不同的 prompt 模板，如 "id_photo"、"passport" 等
     * 可选，默认由服务端根据配置决定
     */
    private String photoType;
}
