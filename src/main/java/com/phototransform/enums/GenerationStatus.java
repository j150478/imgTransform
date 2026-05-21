package com.phototransform.enums;

/**
 * 图像生成结果状态枚举
 *
 * 描述 doubao-seedream-4.5 模型图像生成请求的执行结果状态。
 * 根据 API 响应中的整体状态进行映射：
 * - 所有图片均成功生成 → SUCCESS
 * - 部分图片生成失败（组图模式） → PARTIAL_SUCCESS
 * - 全部失败或请求异常 → FAILED
 */
public enum GenerationStatus {

    /**
     * 全部生成成功
     * 所有请求的图片均已成功生成，无任何错误
     */
    SUCCESS,

    /**
     * 部分成功（仅组图模式可能出现）
     * 组图生成中部分图片成功、部分失败，失败详情见对应图片的 error 字段
     */
    PARTIAL_SUCCESS,

    /**
     * 全部失败
     * 所有图片均生成失败，或请求本身发生异常，errorMessage 中包含具体原因
     */
    FAILED;
}
