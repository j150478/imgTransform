package com.phototransform.dto;

import com.phototransform.enums.GenerationCapability;
import com.phototransform.enums.GenerationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图像生成结果 DTO
 *
 * 封装 doubao-seedream-4.5 模型图像生成的结果信息。
 *
 * 结果状态说明：
 * - SUCCESS: 生成成功，images 中包含生成的图片
 * - PARTIAL_SUCCESS: 部分成功（组图模式下部分图片生成失败）
 * - FAILED: 生成失败，errorMessage 中包含错误信息
 *
 * @author PhotoTransform Team
 * @since 1.0.0
 * @see ImageGenerationRequest
 * @see GenerationCapability
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationResult {

    /**
     * 任务ID
     *
     * 本次生成任务的唯一标识符，格式：SD + 16位随机字符（大写）。
     *
     * 示例：SD7A3F9B2E1C8D5A4
     *
     * 用途：
     * - 日志追踪：通过任务ID关联完整调用链路
     * - 问题排查：根据任务ID查询详细日志
     * - 结果关联：将请求与结果关联
     */
    private String taskId;

    /**
     * 生成状态
     *
     * 表示本次生成任务的执行状态。
     *
     * 可选值：
     * - SUCCESS: 生成成功，所有图片均已生成
     * - PARTIAL_SUCCESS: 部分成功（仅组图模式可能出现），部分图片生成失败
     * - FAILED: 生成失败，未生成任何图片
     *
     * 注意：组图模式下，即使 status 为 SUCCESS，也建议检查每张图片的 error 字段，
     * 因为某些图片可能因审核等原因生成失败。
     */
    private GenerationStatus status;

    /**
     * 生成的图像列表
     *
     * 包含本次生成的所有图像信息。
     *
     * 说明：
     * - 单图生成：列表中只有1张图片（index=0）
     * - 组图生成：列表中包含多张图片（index 从 0 到 n-1）
     * - 部分成功：失败的图片也会有记录，但 url/b64Json 为 null，error 字段包含错误信息
     *
     * @see GeneratedImage
     */
    @Builder.Default
    private List<GeneratedImage> images = new ArrayList<>();

    /**
     * 实际使用的生成能力
     *
     * 记录本次生成实际使用的 doubao-seedream-4.5 生成能力。
     *
     * 用途：
     * - 日志记录：便于追踪实际调用的能力
     * - 调试排查：确认是否按预期使用指定能力
     * - 统计分析：统计各能力的使用频率
     *
     * @see GenerationCapability
     */
    private GenerationCapability usedCapability;

    /**
     * 请求创建时间
     *
     * 记录本次生成请求的创建时间。
     *
     * 格式：LocalDateTime，ISO-8601格式（如 2024-01-15T09:30:00）
     *
     * 用途：
     * - 计算总耗时：completedAt - createdAt
     * - 记录追踪：关联请求和日志
     * - 统计分析：统计各时段的生成量
     */
    private LocalDateTime createdAt;

    /**
     * 生成完成时间
     *
     * 记录本次生成完成的实际时间。
     *
     * 格式：LocalDateTime，ISO-8601格式
     *
     * 说明：
     * - 成功时：记录最后一张图片生成完成的时间
     * - 失败时：记录失败发生的时间
     * - 部分成功时：记录最后处理完成的时间
     *
     * 用途：结合 createdAt 计算实际生成耗时
     */
    private LocalDateTime completedAt;

    /**
     * 实际使用的模型
     *
     * 记录本次生成实际使用的模型版本。
     *
     * 用途：
     * - 确认实际调用的模型版本
     * - 问题排查时确认是否使用了正确的模型
     * - 统计分析各模型的使用量
     *
     * 示例：doubao-seedream-4-5-251128
     */
    private String model;

    /**
     * 原始提示词
     *
     * 记录本次生成使用的原始提示词（即请求中的 prompt）。
     *
     * 用途：
     * - 结果追溯：根据提示词关联请求和结果
     * - 调试排查：确认实际使用的提示词内容
     * - 日志记录：完整的调用上下文
     *
     * 注意：如果使用了提示词优化功能，
     * 实际发送给模型的可能是优化后的提示词（可在 GeneratedImage#getRevisedPrompt() 中查看）。
     *
     * @see GeneratedImage#getRevisedPrompt()
     */
    private String prompt;

    /**
     * 错误信息
     *
     * 当生成失败（status = FAILED）或部分成功（status = PARTIAL_SUCCESS）时，
     * 记录错误描述信息。
     *
     * 常见错误类型：
     * - 参数错误：提示词过长、参考图数量不符、生成数量超限等
     * - 审核失败：提示词或生成内容触发安全审核
     * - 服务错误：模型服务异常、超时、资源不足等
     * - 网络错误：参考图下载失败、网络超时等
     *
     * 注意：部分成功时，错误信息描述整体情况，
     * 具体每张图片的失败原因请查看对应 GeneratedImage 的 error 字段。
     *
     * @see #errorCode
     * @see GeneratedImage#getError()
     */
    private String errorMessage;

    /**
     * 错误码
     *
     * 当生成失败时，记录机器可读的错误码，便于程序处理。
     *
     * 常见错误码：
     * - 400: 请求参数错误 - 检查提示词长度、参考图数量等参数
     * - 401: 认证失败 - 检查API Key是否有效
     * - 403: 内容审核失败 - 修改提示词，避免敏感内容
     * - 429: 请求过于频繁 - 降低请求频率，或联系扩容
     * - 500: 服务内部错误 - 稍后重试，或联系技术支持
     * - 504: 生成超时 - 简化提示词，或减少生成数量
     *
     * @see #errorMessage
     */
    private String errorCode;

    /**
     * 生成的单张图像信息
     *
     * 封装单张生成图片的详细信息和元数据。
     *
     * @see #images
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedImage {

        /**
         * 图像索引
         *
         * 在组图生成中，表示图片的顺序编号（从0开始）。
         *
         * 示例：
         * - 单图生成：index = 0
         * - 组图生成（4张）：index = 0, 1, 2, 3
         *
         * 用途：在组图结果中标识图片顺序，便于前端按序展示
         */
        private Integer index;

        /**
         * 图像URL
         *
         * 生成图片的下载链接。仅在 responseFormat = "url" 时返回。
         *
         * 有效期：链接在图片生成后24小时内有效，请及时下载保存
         *
         * 为空的情况：
         * - responseFormat = "b64_json" 时（使用 b64Json 字段）
         * - 该图片生成失败时（查看 error 字段）
         *
         * @see #b64Json
         * @see #getError()
         */
        private String url;

        /**
         * Base64编码图像数据
         *
         * 生成图片的Base64编码字符串。仅在 responseFormat = "b64_json" 时返回。
         *
         * 格式：不包含 data URI scheme 前缀，仅包含纯Base64编码数据
         *
         * 使用示例：
         * // 前端展示
         * img.src = "data:image/png;base64," + image.b64Json;
         *
         * // 后端保存
         * byte[] imageData = Base64.getDecoder().decode(image.getB64Json());
         * Files.write(Paths.get("image.png"), imageData);
         *
         * 为空的情况：
         * - responseFormat = "url" 时（使用 url 字段）
         * - 该图片生成失败时（查看 error 字段）
         *
         * @see #url
         * @see #getError()
         */
        private String b64Json;

        /**
         * 图像内容类型
         *
         * 生成图片的MIME类型，通常为 "image/png" 或 "image/jpeg"。
         *
         * 常见值：
         * - image/png: PNG格式，支持透明背景，无损压缩
         * - image/jpeg: JPEG格式，有损压缩，适合照片
         * - image/webp: WebP格式，高压缩率，支持透明
         *
         * 用途：
         * - 设置HTTP响应头 Content-Type
         * - 前端根据类型选择渲染方式
         * - 保存文件时确定扩展名
         */
        private String contentType;

        /**
         * 图像宽度（像素）
         *
         * 生成图片的实际宽度，单位为像素。
         *
         * 说明：
         * - 实际尺寸可能与请求的尺寸略有差异
         * - 模型会根据请求的尺寸选择最接近的预设尺寸
         * - 具体尺寸取决于模型内部的尺寸表
         *
         * 常见值：512, 768, 1024, 1536, 2048, 2560, 4096 等
         *
         * @see #height
         */
        private Integer width;

        /**
         * 图像高度（像素）
         *
         * 生成图片的实际高度，单位为像素。
         *
         * 说明：参见 {@link #width} 的说明
         *
         * @see #width
         */
        private Integer height;

        /**
         * 文件大小（字节）
         *
         * 生成图片的文件大小，单位为字节（Byte）。
         *
         * 换算参考：
         * - 1024 B = 1 KB
         * - 1024 KB = 1 MB
         * - 典型PNG图片：1-5 MB（1024x1024）
         *
         * 用途：
         * - 存储规划：预估存储空间需求
         * - 传输预估：计算上传下载时间
         * - 成本控制：按流量计费场景
         */
        private Long fileSize;

        /**
         * 优化后的提示词
         *
         * 如果启用了提示词优化功能，此处显示优化后的实际发送给模型的提示词。
         *
         * 说明：
         * - 仅在启用提示词优化时返回
         * - 优化后的提示词可能比原始提示词更长、更详细
         * - 模型实际使用的是优化后的提示词
         *
         * 用途：
         * - 学习参考：了解模型偏好的提示词写法
         * - 调试优化：对比优化前后的效果差异
         * - 问题排查：确认模型实际接收的提示词
         *
         * @see ImageGenerationRequest#getPrompt()
         */
        private String revisedPrompt;

        /**
         * 生成错误信息
         *
         * 当该图片生成失败时，记录错误描述。
         *
         * 说明：
         * - 仅在单张图片生成失败时返回（组图模式可能出现部分失败）
         * - 单图模式下，如果生成失败，此字段包含失败原因
         * - 组图模式下，部分成功时，失败的图片会在此记录具体错误
         *
         * 常见错误：
         * - 审核失败：内容触发安全审核
         * - 生成超时：图片生成超时
         * - 资源不足：服务端资源紧张
         *
         * @see ImageGenerationResult#errorMessage
         * @see GeneratedImage#getError()
         */
        private String error;
    }
}
