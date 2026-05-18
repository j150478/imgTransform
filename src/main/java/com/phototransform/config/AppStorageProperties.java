package com.phototransform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用存储配置
 *
 * 读取 app.storage.* 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.storage")
public class AppStorageProperties {

    /** 存储类型（local/oss/cos） */
    private String type = "local";

    /** 本地存储路径 */
    private String localPath = "uploads/";

    /** 访问 URL 前缀 */
    private String urlPrefix = "http://localhost:8080/uploads/";
}
