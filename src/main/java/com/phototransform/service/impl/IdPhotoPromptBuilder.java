package com.phototransform.service.impl;

import com.phototransform.enums.BackgroundColor;
import org.springframework.stereotype.Component;

/**
 * 证件照 prompt 构建器，负责将背景色参数转化为 Seedream 可用的生成提示词
 */
@Component
public class IdPhotoPromptBuilder {

    /**
     * 根据背景色构建证件照生成 prompt
     *
     * @param backgroundColor 背景色枚举
     * @return 构建好的 prompt 字符串
     */
    public String build(BackgroundColor backgroundColor) {
        return "Turn this photo into a standard ID photo:\n"
                + "- Background: " + backgroundColor.getPromptDescription() + "\n"
                + "- Person: centered, facing forward, symmetrical face\n"
                + "- Show full head and shoulders\n"
                + "- Lighting: even and natural\n"
                + "- Clear face, sharp eyes, natural skin texture\n"
                + "- Clothes: replace with black suit, white shirt, and black tie\n"
                + "- Formal style, neat and professional\n"
                + "- Consistent details: ears, shoulders, tie, collar must be aligned and symmetrical\n"
                + "- Final style: official passport/ID photo, high resolution, clean and sharp\n"
                + "\n"
                + "No artistic effects\n"
                + "No filters\n"
                + "No distortions\n"
                + "No asymmetry in face or body\n"
                + "No extra objects or backgrounds\n"
                + "No duplicated or missing body parts\n"
                + "No clothing artifacts (broken tie, missing collar, misaligned suit)\n"
                + "No cropped head or missing shoulders\n"
                + "No unrealistic lighting\n"
                + "No blurriness or low resolution";
    }
}
