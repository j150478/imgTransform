package com.phototransform.enums;

import lombok.Getter;

/**
 * 证件照背景颜色枚举
 *
 * 前端传 code（1/2/3），后端映射为颜色名和 RGB 值
 */
@Getter
public enum BackgroundColor {

    BLUE(1, "blue", "0,112,192"),
    RED(2, "red", "255,0,0"),
    WHITE(3, "white", "255,255,255");

    /**
     * 颜色代码（前端传入的标识）
     * 1 = 蓝色, 2 = 红色, 3 = 白色
     */
    private final int code;

    /**
     * 颜色英文名称
     * 用于 Seedream API 的背景色参数
     */
    private final String name;

    /**
     * RGB 颜色值
     * 格式为 "R,G,B"，用于图像处理时的颜色替换
     */
    private final String rgb;

    BackgroundColor(int code, String name, String rgb) {
        this.code = code;
        this.name = name;
        this.rgb = rgb;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 前端传入的颜色代码
     * @return 对应的背景颜色枚举
     * @throws IllegalArgumentException 当 code 无效时抛出
     */
    public static BackgroundColor fromCode(int code) {
        for (BackgroundColor color : values()) {
            if (color.code == code) {
                return color;
            }
        }
        throw new IllegalArgumentException("无效的背景颜色代码: " + code);
    }

}
