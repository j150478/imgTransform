package com.phototransform.dto;

import com.phototransform.enums.ModelType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 照片转化请求 DTO
 * 
 * 用于接收用户提交的证件照转化请求参数
 */
@Data
public class PhotoTransformRequest {

    /**
     * 用户上传的照片文件
     * 应为图片格式（JPEG、PNG等）
     */
    private MultipartFile file;

    /**
     * 图像处理模型类型
     * 指定使用哪种 AI 模型进行证件照生成
     * @see ModelType
     */
    private ModelType modelType;

    /**
     * 证件照背景颜色
     * 如：white（白色）、blue（蓝色）、red（红色）
     */
    private String backgroundColor;

    /**
     * 证件照类型
     * 如：id_card（身份证）、passport（护照）、driver_license（驾照）
     */
    private String photoType;
}
