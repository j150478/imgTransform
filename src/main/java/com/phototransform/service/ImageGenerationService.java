package com.phototransform.service;

import com.phototransform.dto.TextToImageRequest;
import com.phototransform.dto.TextToImageResponse;

/**
 * 文生图服务接口
 */
public interface ImageGenerationService {

    /**
     * 创建文生图任务
     */
    TextToImageResponse createTask(TextToImageRequest request, Long userId);

    /**
     * 执行文生图处理
     */
    void processTask(String taskId);

    /**
     * 查询文生图结果
     */
    TextToImageResponse queryResult(String taskId);
}
