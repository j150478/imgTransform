package com.phototransform.common;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 任务 ID 生成器，统一 ID 生成策略
 * <p>
 * 格式：前缀 + 16 位大写字母数字（UUID 去连字符取前 16 位）
 */
@Component
public class TaskIdGenerator {

    /**
     * 生成任务 ID
     *
     * @param prefix 前缀（如 PT、SD）
     * @return 格式：prefix + 16 位大写 UUID 子串
     */
    public String generate(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
