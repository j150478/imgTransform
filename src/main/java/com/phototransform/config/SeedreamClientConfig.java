package com.phototransform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 火山引擎 Seedream 客户端配置
 *
 * 配置 Doubao Seedream 图像生成模型 API 的连接参数
 * 通过 application.yml 中的 seedream 前缀进行配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "seedream")
public class SeedreamClientConfig {

    /**
     * 火山引擎 API 访问密钥 ID
     * 用于身份验证，从火山引擎控制台获取
     */
    private String accessKeyId;

    /**
     * 火山引擎 API 访问密钥 Secret
     * 用于身份验证，从火山引擎控制台获取
     */
    private String accessKeySecret;

    /**
     * API 端点地址
     * 火山引擎 Seedream 服务的服务地址
     * 默认值为：seedream.volces.com
     */
    private String endpoint = "seedream.volces.com";

    /**
     * 连接超时时间（毫秒）
     * 建立连接的最大等待时间
     * 默认值为 10000 毫秒（10秒）
     */
    private Integer connectionTimeout = 10000;

    /**
     * 读取超时时间（毫秒）
     * 等待响应的最大等待时间
     * 默认值为 60000 毫秒（60秒）
     */
    private Integer readTimeout = 60000;
}
