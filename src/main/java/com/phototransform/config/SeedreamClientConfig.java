package com.phototransform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 火山引擎 Seedream 客户端配置
 */
@Component
@ConfigurationProperties(prefix = "seedream")
public class SeedreamClientConfig {

    /** API Key，建议从环境变量 ARK_API_KEY 读取 */
    private String apiKey;

    /** Base URL，默认: https://ark.cn-beijing.volces.com/api/v3 */
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";

    /** 默认模型，默认: doubao-seedream-4-5-251128 */
    private String modelName = "doubao-seedream-4-5-251128";

    /** 默认图像尺寸，默认: 1024x1024 */
    private String defaultSize = "1024x1024";

    /** 默认响应格式: url 或 b64_json，默认: url */
    private String defaultResponseFormat = "url";

    /** 连接超时（毫秒），默认: 10000 */
    private Integer connectionTimeout = 10000;

    /** 读取超时（毫秒），默认: 60000 */
    private Integer readTimeout = 60000;

    /** 是否开启水印，默认: false */
    private Boolean defaultWatermark = false;

    /** 组图生成模式: auto 或 disabled，默认: disabled */
    private String defaultSequentialGeneration = "disabled";

    // ==================== Getter 方法 ====================

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getDefaultSize() {
        return defaultSize;
    }

    public void setDefaultSize(String defaultSize) {
        this.defaultSize = defaultSize;
    }

    public String getDefaultResponseFormat() {
        return defaultResponseFormat;
    }

    public void setDefaultResponseFormat(String defaultResponseFormat) {
        this.defaultResponseFormat = defaultResponseFormat;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Boolean getDefaultWatermark() {
        return defaultWatermark;
    }

    public void setDefaultWatermark(Boolean defaultWatermark) {
        this.defaultWatermark = defaultWatermark;
    }

    public String getDefaultSequentialGeneration() {
        return defaultSequentialGeneration;
    }

    public void setDefaultSequentialGeneration(String defaultSequentialGeneration) {
        this.defaultSequentialGeneration = defaultSequentialGeneration;
    }
}
