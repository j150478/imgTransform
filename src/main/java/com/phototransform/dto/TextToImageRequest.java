package com.phototransform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 文生图请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextToImageRequest {

    @NotBlank(message = "提示词不能为空")
    @Size(max = 2000, message = "提示词长度不能超过2000个字符")
    private String prompt;

    private String size;

    @Builder.Default
    private Integer n = 1;
}
