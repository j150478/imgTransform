package com.phototransform.service;

import com.phototransform.dto.ImageGenerationRequest;
import com.phototransform.dto.ImageGenerationResult;

import java.util.List;

/**
 * 火山引擎 Seedream 图像生成服务接口
 */
public interface SeedreamImageService {

    /**
     * 文生图 - 根据文本描述生成单张图像
     *
     * @param request 图像生成请求
     * @return 图像生成结果
     */
    ImageGenerationResult generateImage(ImageGenerationRequest request);

    /**
     * 图生图 - 基于参考图像进行变换或增强
     *
     * @param request 图像生成请求（需包含 referenceImages）
     * @return 图像生成结果
     */
    ImageGenerationResult generateImageWithReference(ImageGenerationRequest request);

    /**
     * 组图生成 - 生成一系列风格一致的关联图像
     *
     * @param request 图像生成请求（需设置 sequentialImageGeneration="auto"）
     * @return 图像生成结果
     */
    ImageGenerationResult generateImageSet(ImageGenerationRequest request);

    /**
     * 批量生成 - 针对多个提示词批量生成图像
     *
     * @param requests 图像生成请求列表
     * @return 图像生成结果列表
     */
    List<ImageGenerationResult> batchGenerateImages(List<ImageGenerationRequest> requests);
}
