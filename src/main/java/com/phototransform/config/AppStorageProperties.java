package com.phototransform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用文件存储配置。
 *
 * <p>读取 {@code app.storage.*} 配置项，用于切换存储后端类型（本地文件系统或 Supabase Storage）、
 * 配置本地存储路径及访问 URL 前缀。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.storage")
public class AppStorageProperties {

    /** 存储类型（local/supabase） */
    private String type = "local";

    /** 本地存储路径 */
    private String localPath = "uploads/";

    /** 访问 URL 前缀 */
    private String urlPrefix = "http://localhost:8080/uploads/";
}
