package com.phototransform.enums;

/**
 * 图像生成模式枚举
 *
 * 定义 doubao-seedream-4.5 模型支持的两种生成模式：
 * - SINGLE: 单图生成（sequential_image_generation=disabled）
 * - SEQUENTIAL: 组图生成（sequential_image_generation=auto）
 *
 * @author PhotoTransform Team
 * @since 1.0.0
 */
public enum GenerationMode {

    /**
     * 单图生成模式
     *
     * 特性：
     * - 每次请求仅生成1张图片
     * - 适用于精确控制单张图像质量的场景
     * - 支持文生图、单图生图、多图生图
     *
     * 使用场景：
     * - 证件照生成
     * - 单张产品图优化
     * - 头像生成
     */
    SINGLE("disabled", "单图生成"),

    /**
     * 组图生成模式
     *
     * 特性：
     * - 每次请求可生成多张关联图片（最多15张）
     * - 图片之间保持风格、主题一致性
     * - 适用于需要多角度、多场景展示的需求
     *
     * 使用场景：
     * - 产品多角度展示
     * - 场景变体生成
     * - 连续动作序列
     * - 主题系列图集
     *
     * 约束：
     * - 输入参考图数量 + 生成图片数量 ≤ 15
     * - 最少生成1张，最多15张
     */
    SEQUENTIAL("auto", "组图生成");

    /**
     * SDK 参数值
     * 用于设置 sequential_image_generation 参数
     */
    private final String sdkValue;

    /**
     * 中文描述
     * 用于日志和前端展示
     */
    private final String description;

    GenerationMode(String sdkValue, String description) {
        this.sdkValue = sdkValue;
        this.description = description;
    }

    /**
     * 获取 SDK 参数值
     *
     * @return sequential_image_generation 参数值（"disabled" 或 "auto"）
     */
    public String getSdkValue() {
        return sdkValue;
    }

    /**
     * 获取中文描述
     *
     * @return 模式的中文描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据 SDK 参数值获取枚举
     *
     * @param sdkValue SDK 参数值（"disabled" 或 "auto"）
     * @return 对应的 GenerationMode 枚举，如果不匹配返回 null
     */
    public static GenerationMode fromSdkValue(String sdkValue) {
        for (GenerationMode mode : values()) {
            if (mode.sdkValue.equalsIgnoreCase(sdkValue)) {
                return mode;
            }
        }
        return null;
    }

    /**
     * 判断是否为组图模式
     *
     * @return true 如果是组图模式（SEQUENTIAL）
     */
    public boolean isSequential() {
        return this == SEQUENTIAL;
    }

    /**
     * 判断是否为单图模式
     *
     * @return true 如果是单图模式（SINGLE）
     */
    public boolean isSingle() {
        return this == SINGLE;
    }
}
