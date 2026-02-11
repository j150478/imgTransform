package com.phototransform.dto;

import com.phototransform.enums.GenerationCapability;
import com.phototransform.enums.GenerationMode;

import javax.validation.constraints.*;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图像生成请求 DTO
 *
 * 封装 doubao-seedream-4.5 模型的图像生成请求参数。
 *
 * 核心参数说明：
 * - prompt: 图像生成提示词，必填，建议不超过300汉字或600英文单词
 * - mode: 生成模式，SINGLE（单图）或 SEQUENTIAL（组图）
 * - capability: 生成能力，显式指定时使用
 * - referenceImages: 参考图列表，支持1-14张
 * - n: 生成数量，单图模式固定为1，组图模式1-15，默认4
 *
 * @author PhotoTransform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationRequest {

    /**
     * 生成提示词
     *
     * 描述要生成的图像内容，支持中文和英文。
     *
     * 建议：不超过300个汉字或600个英文单词。字数过多信息容易分散，
     * 模型可能忽略细节，只关注重点，造成图片缺失部分元素。
     *
     * 提示词技巧：
     * - 主体描述：明确主体是什么，如"一只橘色猫咪"
     * - 场景描述：描述环境和背景，如"在阳光明媚的草地上"
     * - 风格描述：指定艺术风格，如"油画风格"、"赛博朋克"
     * - 细节修饰：添加细节要求，如"8K超清"、"精致细节"
     *
     * 必填
     */
    @NotBlank(message = "生成提示词不能为空")
    @Size(max = 600, message = "提示词长度不能超过600个字符")
    private String prompt;

    /**
     * 负面提示词
     *
     * 描述不希望出现在图像中的元素。模型会尽量避免生成这些内容。
     *
     * 适用场景：
     * - 排除特定物体：如"人、文字、水印"
     * - 避免特定风格：如"低质量、模糊、扭曲"
     * - 控制画面内容：如"杂乱背景、多余肢体"
     *
     * 可选，默认 null
     */
    @Size(max = 300, message = "负面提示词长度不能超过300个字符")
    private String negativePrompt;

    /**
     * 参考图像列表
     *
     * 用于图生图、多图生图等场景。支持URL或Base64编码。
     *
     * 格式要求：
     * - 图片URL：确保URL可公开访问，建议设置较长的有效期
     * - Base64编码：格式为 data:image/<格式>;base64,<编码>
     *   - 图片格式需小写，如 data:image/png;base64,iVBORw0KG...
     *   - 支持的格式：jpeg、png、webp、bmp、tiff、gif
     *
     * 图片要求：
     * - 数量限制：1-14张（根据具体能力）
     * - 单张大小：不超过10MB
     * - 总像素：不超过6000x6000
     * - 宽高比：1/16 到 16 之间
     *
     * 可选，根据具体能力决定
     */
    @Size(min = 0, max = 14, message = "参考图数量必须在0-14之间")
    private List<String> referenceImages;

    /**
     * 生成模式
     *
     * 指定生成单图还是组图，对应 doubao-seedream-4.5 模型的 sequential_image_generation 参数。
     *
     * 可选值：
     * - SINGLE（disabled）：单图生成模式，每次生成1张图片
     * - SEQUENTIAL（auto）：组图生成模式，每次可生成多张关联图片
     *
     * 选择建议：
     * - 需要单张高质量图片 → SINGLE
     * - 需要系列图、多角度、场景变体 → SEQUENTIAL
     *
     * 可选，默认根据 n 自动判断（n > 1 则 SEQUENTIAL，否则 SINGLE）
     */
    private GenerationMode mode;

    /**
     * 显式指定的生成能力
     *
     * 可选参数，显式指定使用哪种生成能力。如果不指定，系统会根据参考图数量和生成模式自动判断。
     *
     * 适用场景：
     * - 需要精确控制生成方式
     * - 避免自动判断的不确定性
     * - 使用特定优化参数的能力
     *
     * 可选值：
     * - TEXT_TO_IMAGE - 文生图
     * - TEXT_TO_IMAGE_SET - 文生组图
     * - SINGLE_IMAGE_TO_IMAGE - 单图生图
     * - SINGLE_IMAGE_TO_IMAGE_SET - 单图生组图
     * - MULTI_IMAGE_TO_IMAGE - 多图生图
     * - MULTI_IMAGE_TO_IMAGE_SET - 多图生组图
     *
     * 可选，默认 null（自动判断）
     */
    private GenerationCapability capability;

    /**
     * 生成图像数量
     *
     * 指定要生成的图像数量，仅对组图模式有效。
     *
     * 约束条件：
     * - 单图模式（SINGLE）：固定为1，此参数无效
     * - 组图模式（SEQUENTIAL）：1-15，默认为4
     * - 组图总数限制：输入参考图数量 + 生成图片数量 ≤ 15
     *
     * 建议：
     * - 4-6张：适合展示多角度/场景
     * - 8-12张：适合完整系列展示
     * - 考虑API调用成本，按需设置
     *
     * 可选，默认 4（组图模式）/ 1（单图模式，固定值）
     */
    @Min(value = 1, message = "生成数量至少为1")
    @Max(value = 15, message = "生成数量最多为15")
    private Integer n;

    /**
     * 图像尺寸
     *
     * 指定生成图像的尺寸，支持两种方式：
     *
     * 方式一：分辨率标识（推荐）
     * - "1K" - 约1024x1024
     * - "2K" - 约2048x2048
     * - "4K" - 约4096x4096
     *
     * 方式二：具体像素值
     * - 格式："宽x高"，如 "1024x1024"、"2048x1152"
     * - 总像素范围：[1280x720=921600, 4096x4096=16777216]
     * - 宽高比范围：[1/16, 16]
     *
     * 常用尺寸推荐：
     * - 头像/图标：1024x1024（1:1比例，适合头像）
     * - 产品图：2048x2048（2K高清，适合电商）
     * - 风景/壁纸：2560x1440（16:9宽屏，适合桌面）
     * - 海报/印刷：4096x4096（4K超清，适合印刷）
     *
     * 可选，默认从配置文件读取（默认1024x1024）
     */
    private String size;

    /**
     * 模型版本
     *
     * 指定使用的 doubao-seedream 模型版本。
     *
     * 可选值：
     * - "doubao-seedream-4-5-251128" - doubao-seedream-4.5（推荐）
     * - "doubao-seedream-4-0-250828" - doubao-seedream-4.0
     *
     * 版本特性对比：
     * - 文生图：4.5 ✓  4.0 ✓
     * - 图生图：4.5 ✓  4.0 ✓
     * - 多图生图：4.5 ✓  4.0 ✓
     * - 组图生成：4.5 ✓  4.0 ✓
     * - 图像质量：4.5 更高  4.0 高
     * - 生成速度：4.5 更快  4.0 快
     *
     * 选择建议：
     * - 新项目：使用 4.5（推荐）
     * - 已有项目：可根据需要升级，API兼容
     * - 特定需求：如需使用4.0特有特性（如有）
     *
     * 可选，默认从配置文件读取（默认 doubao-seedream-4-5-251128）
     */
    private String model;

    /**
     * 响应格式
     *
     * 指定生成图像的返回格式。
     *
     * 可选值：
     * - "url" - 返回图片下载链接（推荐）
     *   - 链接在图片生成后24小时内有效
     *   - 需要及时下载保存
     *   - 适用于图片需要存储到自有存储的场景
     * - "b64_json" - 返回 Base64 编码的图片数据
     *   - 直接返回图片数据，无需二次下载
     *   - 响应体较大，可能影响传输性能
     *   - 适用于图片直接使用、无需存储的场景
     *
     * 选择建议：
     * - 图片需长期存储：url（下载后存自有存储，不受24小时限制）
     * - 图片直接使用：b64_json（无需二次请求，即时使用）
     * - 批量生成：url（响应体小，传输效率高）
     * - 实时展示：b64_json（减少请求延迟，提升用户体验）
     *
     * 可选，默认 "url"（从配置文件读取，默认url）
     */
    private String responseFormat;

    /**
     * 是否开启水印
     *
     * 控制是否在生成的图片中添加 "AI生成" 字样的水印标识。
     *
     * 可选值：
     * - true - 在图片右下角添加 "AI生成" 水印（默认）
     * - false - 不添加水印
     *
     * 选择建议：
     * - 对外公开展示：true（符合AI生成内容标识规范）
     * - 内部测试：false（无标识干扰，便于评估）
     * - 商业发布：false（自行添加品牌标识）
     * - 社交媒体分享：true（提示内容来源，避免误解）
     *
     * 可选，默认 false（从配置文件读取，默认false）
     */
    private Boolean watermark;

    /**
     * 任务超时时间（毫秒）
     *
     * 单个生成任务的超时时间。如果超过此时间任务未完成，将抛出超时异常。
     *
     * 建议值：
     * - 单图生成：30000-60000ms（30-60秒）
     * - 组图生成（4张）：60000-120000ms（60-120秒）
     * - 大图（4K）：适当延长
     *
     * 可选，默认 60000（60秒）
     */
    @Min(value = 10000, message = "超时时间至少10秒")
    @Max(value = 300000, message = "超时时间最多5分钟")
    private Long timeout = 60000L;

    /**
     * 自定义扩展参数
     *
     * 用于传递额外的参数，如特定版本的特殊参数、调试参数等。
     *
     * 使用说明：
     * - 标准参数应优先使用专用字段
     * - 扩展参数仅用于特殊场景
     * - 不同版本SDK支持的扩展参数可能不同
     *
     * 常见扩展参数（示例）：
     * - seed - 随机种子，用于复现结果
     * - guidance_scale - 文本权重，控制生成自由度
     * - optimize_prompt - 提示词优化选项
     *
     * 可选，默认 null
     * 示例：{"seed": 42, "guidance_scale": 7.5}
     */
    private Map<String, Object> extraParams;
}
