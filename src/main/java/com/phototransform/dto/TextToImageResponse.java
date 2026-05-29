package com.phototransform.dto;

import com.phototransform.enums.ImageTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文生图结果响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextToImageResponse {

    private String taskId;

    private ImageTaskStatus status;

    @Builder.Default
    private List<String> imageUrls = java.util.Collections.emptyList();

    private String errorMessage;
}
