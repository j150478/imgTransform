package com.phototransform.service;

import com.phototransform.dto.ImageGenerationRequest;
import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.enums.GenerationCapability;

import java.util.List;

/**
 * 火山引擎 Seedream 图像生成服务接口
 *
 * <p>基于 doubao-seedream-4.5 模型，支持以下生成能力：</p>
 *
 * <h3>单图生成（sequential_image_generation=disabled）</h3>
 * <ul>
 *   <li>文生图：根据文本描述生成单张图像</li>
 *   <li>单图生图：基于1张参考图生成单张图像</li>
 *   <li>多图生图：融合2-14张参考图生成单张图像</li>
 * </ul>
 *
 * <h3>组图生成（sequential_image_generation=auto）</h3>
 * <ul>
 *   <li>文生组图：根据文本描述生成多张关联图像</li>
 *   <li>单图生组图：基于1张参考图生成多张图像（最多14张）</li>
 *   <li>多图生组图：融合2-14张参考图生成组图</li>
 * </ul>
 *
 * <h3>约束条件</h3>
 * <ul>
 *   <li>参考图数量限制：0（文生图）、1（单图生图）、2-14（多图生图）</li>
 *   <li>组图总数限制：输入参考图数量 + 生成图片数量 ≤ 15</li>
 *   <li>组图模式下，未指定生成数量时默认生成4张</li>
 * </ul>
 *
 * @author PhotoTransform Team
 * @since 1.0.0
 * @see GenerationCapability
 * @see ImageGenerationRequest
 * @see ImageGenerationResult
 */
public interface SeedreamImageService {

    /**
     * 通用图像生成方法
     *
     * <p>根据请求参数自动判断生成能力，支持所有 doubao-seedream-4.5 模型能力：</p>
     *
     * <h3>自动识别逻辑</h3>
     * <ol>
     *   <li>判断参考图数量：
     *     <ul>
     *       <li>0张 → 文生图</li>
     *       <li>1张 → 单图生图</li>
     *       <li>2-14张 → 多图生图</li>
     *     </ul>
     *   </li>
     *   <li>判断生成模式：
     *     <ul>
     *       <li>request.getMode() == SINGLE → 单图生成</li>
     *       <li>request.getMode() == SEQUENTIAL → 组图生成</li>
     *       <li>未指定时根据 request.getN() 判断（N > 1 则组图）</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * <h3>示例用法</h3>
     * <pre>{@code
     * // 文生图
     * ImageGenerationRequest request = ImageGenerationRequest.builder()
     *     .prompt("一只可爱的猫咪")
     *     .mode(GenerationMode.SINGLE)
     *     .build();
     * ImageGenerationResult result = seedreamImageService.generate(request);
     *
     * // 图生图（单图）
     * ImageGenerationRequest request = ImageGenerationRequest.builder()
     *     .prompt("将这张图片转换成油画风格")
     *     .referenceImages(Collections.singletonList(imageUrl))
     *     .mode(GenerationMode.SINGLE)
     *     .build();
     *
     * // 文生组图
     * ImageGenerationRequest request = ImageGenerationRequest.builder()
     *     .prompt("一只可爱的猫咪在不同场景下")
     *     .mode(GenerationMode.SEQUENTIAL)
     *     .n(4)  // 生成4张组图
     *     .build();
     * }</pre>
     *
     * @param request 图像生成请求，包含提示词、参考图、生成模式等参数
     * @return 图像生成结果，包含生成的图片列表和元数据
     * @throws com.phototransform.common.BusinessException 当参数校验失败或生成失败时抛出
     * @see #generateWithCapability(ImageGenerationRequest, GenerationCapability)
     * @see #generateSingle(ImageGenerationRequest)
     * @see #generateSequential(ImageGenerationRequest)
     */
    ImageGenerationResult generate(ImageGenerationRequest request);

    /**
     * 指定生成能力的图像生成
     *
     * <p>显式指定使用哪种 doubao-seedream-4.5 生成能力，适用于需要精确控制生成方式的场景。</p>
     *
     * <h3>支持的生成能力</h3>
     * <table border="1">
     *   <tr><th>能力</th><th>参考图数量</th><th>生成模式</th><th>说明</th></tr>
     *   <tr><td>TEXT_TO_IMAGE</td><td>0</td><td>SINGLE</td><td>文生图</td></tr>
     *   <tr><td>TEXT_TO_IMAGE_SET</td><td>0</td><td>SEQUENTIAL</td><td>文生组图</td></tr>
     *   <tr><td>SINGLE_IMAGE_TO_IMAGE</td><td>1</td><td>SINGLE</td><td>单图生图</td></tr>
     *   <tr><td>SINGLE_IMAGE_TO_IMAGE_SET</td><td>1</td><td>SEQUENTIAL</td><td>单图生组图</td></tr>
     *   <tr><td>MULTI_IMAGE_TO_IMAGE</td><td>2-14</td><td>SINGLE</td><td>多图生图</td></tr>
     *   <tr><td>MULTI_IMAGE_TO_IMAGE_SET</td><td>2-14</td><td>SEQUENTIAL</td><td>多图生组图</td></tr>
     * </table>
     *
     * <h3>示例用法</h3>
     * <pre>{@code
     * // 明确使用多图生图能力（融合多张参考图生成单张图片）
     * ImageGenerationRequest request = ImageGenerationRequest.builder()
     *     .prompt("将这些图片的风格融合到一张图中")
     *     .referenceImages(Arrays.asList(imageUrl1, imageUrl2, imageUrl3))
     *     .build();
     *
     * ImageGenerationResult result = seedreamImageService.generateWithCapability(
     *     request,
     *     GenerationCapability.MULTI_IMAGE_TO_IMAGE
     * );
     *
     * // 明确使用单图生组图能力（基于单张参考图生成系列图）
     * ImageGenerationRequest request = ImageGenerationRequest.builder()
     *     .prompt("基于这张图片生成不同场景下的变体")
     *     .referenceImages(Collections.singletonList(imageUrl))
     *     .n(6)  // 生成6张组图
     *     .build();
     *
     * ImageGenerationResult result = seedreamImageService.generateWithCapability(
     *     request,
     *     GenerationCapability.SINGLE_IMAGE_TO_IMAGE_SET
     * );
     * }</pre>
     *
     * @param request    图像生成请求
     * @param capability 指定的生成能力，决定使用哪种生成方式
     * @return 图像生成结果
     * @throws IllegalArgumentException 当指定的能力与请求参数不匹配时抛出
     *                                  （例如：指定 MULTI_IMAGE_TO_IMAGE 但只提供1张参考图）
     * @see GenerationCapability
     * @see #generate(ImageGenerationRequest)
     */
    ImageGenerationResult generateWithCapability(ImageGenerationRequest request, GenerationCapability capability);

    /**
     * 单图生成（简化方法）
     *
     * <p>便捷方法，用于快速生成单张图片。</p>
     *
     * <p>内部调用 {@link #generate(ImageGenerationRequest)}，
     * 并自动设置 mode 为 {@link com.phototransform.enums.GenerationMode#SINGLE}。</p>
     *
     * @param prompt 提示词，描述要生成的图像内容
     * @return 单张图像的生成结果
     * @see #generateSequential(String, Integer)
     * @see #generate(ImageGenerationRequest)
     */
    default ImageGenerationResult generateSingle(String prompt) {
        return generate(ImageGenerationRequest.builder()
                .prompt(prompt)
                .build());
    }

    /**
     * 组图生成（简化方法）
     *
     * <p>便捷方法，用于快速生成组图。</p>
     *
     * <p>内部调用 {@link #generate(ImageGenerationRequest)}，
     * 并自动设置 mode 为 {@link com.phototransform.enums.GenerationMode#SEQUENTIAL}，
     * 以及设置生成数量 n。</p>
     *
     * @param prompt 提示词，描述要生成的图像内容
     * @param n      生成图片数量（1-15）
     * @return 多张图像的生成结果
     * @throws IllegalArgumentException 当 n 不在有效范围内时抛出
     * @see #generateSingle(String)
     * @see #generate(ImageGenerationRequest)
     */
    default ImageGenerationResult generateSequential(String prompt, Integer n) {
        if (n == null || n < 1 || n > 15) {
            throw new IllegalArgumentException("组图生成数量必须在1-15之间，当前: " + n);
        }
        return generate(ImageGenerationRequest.builder()
                .prompt(prompt)
                .n(n)
                .build());
    }
}
