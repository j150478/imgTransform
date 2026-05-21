package com.phototransform.enums;

/**
 * 图像生成能力枚举
 *
 * 定义 doubao-seedream-4.5 模型支持的具体生成能力。
 * 根据参考图数量和生成模式，划分出不同的生成能力。
 *
 * @author PhotoTransform Team
 * @since 1.0.0
 */
public enum GenerationCapability {

    /**
     * 文生图（纯文本生成单图）
     *
     * 特性：
     * - 仅使用文本提示词生成单张图像
     * - 无需参考图
     * - 适用于创意生成、场景描述等
     *
     * 使用场景：
     * - 创意插画生成
     * - 场景描述可视化
     * - 概念设计
     */
    TEXT_TO_IMAGE(0, GenerationMode.SINGLE, "文生图"),

    /**
     * 文生组图（纯文本生成组图）
     *
     * 特性：
     * - 仅使用文本提示词生成多张关联图像
     * - 无需参考图
     * - 图片之间保持风格、主题一致性
     *
     * 使用场景：
     * - 系列插画生成
     * - 多角度场景展示
     * - 主题变体生成
     */
    TEXT_TO_IMAGE_SET(0, GenerationMode.SEQUENTIAL, "文生组图"),

    /**
     * 单图生图（基于单张参考图生成）
     *
     * 特性：
     * - 使用1张参考图作为输入
     * - 生成单张变换后的图像
     * - 保持参考图的主体特征
     *
     * 使用场景：
     * - 风格迁移
     * - 图像增强
     * - 细节调整
     */
    SINGLE_IMAGE_TO_IMAGE(1, GenerationMode.SINGLE, "单图生图"),

    /**
     * 单图生组图（基于单张参考图生成组图）
     *
     * 特性：
     * - 使用1张参考图作为输入
     * - 生成多张关联图像
     * - 最多生成14张图片
     *
     * 使用场景：
     * - 多角度变体
     * - 场景扩展
     * - 系列图生成
     */
    SINGLE_IMAGE_TO_IMAGE_SET(1, GenerationMode.SEQUENTIAL, "单图生组图"),

    /**
     * 多图生图（基于多张参考图融合生成单图）
     *
     * 特性：
     * - 使用2-14张参考图作为输入
     * - 融合多张图的特征生成单张图像
     * - 适用于风格融合、元素组合
     *
     * 使用场景：
     * - 多元素融合
     * - 风格混合
     * - 创意组合
     */
    MULTI_IMAGE_TO_IMAGE(2, GenerationMode.SINGLE, "多图生图"),

    /**
     * 多图生组图（基于多张参考图生成组图）
     *
     * 特性：
     * - 使用2-14张参考图作为输入
     * - 融合特征并生成多张关联图像
     * - 输入图 + 生成图总数 ≤ 15
     *
     * 使用场景：
     * - 复杂场景变体
     * - 多元素组合系列
     * - 高级创意生成
     */
    MULTI_IMAGE_TO_IMAGE_SET(2, GenerationMode.SEQUENTIAL, "多图生组图");

    /**
     * 参考图数量要求
     * 0: 不需要参考图（文生图）
     * 1: 需要1张参考图
     * 2: 需要2-14张参考图
     */
    private final int referenceImageRequirement;

    /**
     * 对应的生成模式
     */
    private final GenerationMode generationMode;

    /**
     * 中文描述
     */
    private final String description;

    GenerationCapability(int referenceImageRequirement, GenerationMode generationMode, String description) {
        this.referenceImageRequirement = referenceImageRequirement;
        this.generationMode = generationMode;
        this.description = description;
    }

    /**
     * 获取参考图数量要求
     *
     * @return 0: 无需参考图, 1: 需1张, 2: 需多张(2-14)
     */
    public int getReferenceImageRequirement() {
        return referenceImageRequirement;
    }

    /**
     * 获取对应的生成模式
     *
     * @return SINGLE 或 SEQUENTIAL
     */
    public GenerationMode getGenerationMode() {
        return generationMode;
    }

    /**
     * 获取中文描述
     *
     * @return 能力的中文描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 判断是否为组图生成能力
     *
     * @return true 如果 generationMode 是 SEQUENTIAL
     */
    public boolean isSequential() {
        return generationMode == GenerationMode.SEQUENTIAL;
    }

    /**
     * 验证参考图数量是否满足要求
     *
     * @param referenceImageCount 实际参考图数量
     * @return true 如果数量符合要求
     */
    public boolean validateReferenceImageCount(int referenceImageCount) {
        switch (referenceImageRequirement) {
            case 0:
                // 文生图：不需要参考图
                return referenceImageCount == 0;
            case 1:
                // 单图生图：需要1张
                return referenceImageCount == 1;
            case 2:
                // 多图生图：需要2-14张
                return referenceImageCount >= 2 && referenceImageCount <= 14;
            default:
                return false;
        }
    }

    /**
     * 获取参考图数量要求描述
     *
     * @return 人类可读的参考图数量要求说明
     */
    public String getReferenceImageRequirementDesc() {
        switch (referenceImageRequirement) {
            case 0:
                return "无需参考图";
            case 1:
                return "需要1张参考图";
            case 2:
                return "需要2-14张参考图";
            default:
                return "未知要求";
        }
    }
}
