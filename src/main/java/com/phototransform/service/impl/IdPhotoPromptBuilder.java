package com.phototransform.service.impl;

import com.phototransform.enums.BackgroundColor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 证件照 prompt 构建器，根据 photoType 从配置加载模板并渲染
 * <p>
 * 模板配置位于 application.yml 的 prompt.templates 下，
 * 支持 {name}（颜色名）和 {rgb}（RGB 值）两个占位符。
 */
@Component
@ConfigurationProperties(prefix = "prompt")
@Getter
@Setter
public class IdPhotoPromptBuilder {

    /**
     * 模板集合，key 为 photoType，value 为 PromptTemplate
     */
    private Map<String, PromptTemplate> templates = new HashMap<>();

    /**
     * 根据背景色构建证件照生成 prompt（默认使用 id-photo 模板）
     */
    public String build(BackgroundColor backgroundColor) {
        return build("id-photo", backgroundColor);
    }

    /**
     * 根据证件照类型和背景色构建生成 prompt
     *
     * @param photoType       证件照类型，匹配配置中的模板 key（null 或不存时 fallback 到 id-photo）
     * @param backgroundColor 背景色枚举
     * @return 组合后的完整 prompt
     */
    public String build(String photoType, BackgroundColor backgroundColor) {
        PromptTemplate tmpl = resolveTemplate(photoType);
        String system = tmpl.getSystem()
                .replace("{name}", backgroundColor.getName())
                .replace("{rgb}", backgroundColor.getRgb());
        return system + "\n\n" + tmpl.getNegative();
    }

    private PromptTemplate resolveTemplate(String photoType) {
        if (photoType != null && templates.containsKey(photoType)) {
            return templates.get(photoType);
        }
        return templates.get("id-photo");
    }

    /**
     * Prompt 模板 DTO，包含正面指令和负面约束
     */
    @Getter
    @Setter
    public static class PromptTemplate {
        private String system;
        private String negative;
    }
}