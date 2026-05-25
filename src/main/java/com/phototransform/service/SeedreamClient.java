package com.phototransform.service;

import com.phototransform.dto.ImageGenerationRequest;
import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.enums.GenerationCapability;

/**
 * Seedream API 客户端接口。
 *
 * <p>封装火山引擎 Seedream SDK 的调用，提供 DTO 级别的图像生成能力。
 * 实现类必须确保本接口方法永不抛出异常，所有错误通过返回结果中的状态和错误信息传达。</p>
 *
 * @author PhotoTransform Team
 * @since 1.0.0
 */
public interface SeedreamClient {

    /**
     * 执行图像生成请求。
     *
     * <p>将应用层 DTO 转换为 SDK 请求，调用 SDK 并解析响应。
     * 本方法保证不抛出异常——成功时返回 SUCCESS 或 PARTIAL_SUCCESS 状态，
     * 失败时返回 FAILED 状态并携带 errorMessage 和 errorCode 字段。</p>
     *
     * <p>调用方无需处理 SDK 层面的异常，所有异常均会被捕获并转换为结果中的错误信息。</p>
     *
     * @param request    已验证的图像生成请求
     * @param capability 已确定的生成能力
     * @return 图像生成结果，永不抛出异常
     */
    ImageGenerationResult generate(ImageGenerationRequest request, GenerationCapability capability);
}
