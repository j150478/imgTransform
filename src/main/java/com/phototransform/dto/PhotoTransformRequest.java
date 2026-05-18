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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoTransformRequest {

    /** 用户上传的照片文件 */
    @NotNull(message = "上传文件不能为空")
    private MultipartFile file;

    /** 图像处理模型类型 */
    private ModelType modelType;

    /** 证件照背景颜色代码（1=蓝, 2=红, 3=白） */
    private Integer backgroundColor;

    /** 证件照类型 */
    private String photoType;
}
