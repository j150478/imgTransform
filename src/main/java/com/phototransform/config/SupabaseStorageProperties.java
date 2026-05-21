package com.phototransform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Supabase Storage 对象存储配置。
 *
 * <p>读取 {@code supabase.storage.*} 配置项，用于连接 Supabase Storage REST API，
 * 包括项目 URL、服务端 API Key（service_role）及存储桶名称。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "supabase.storage")
public class SupabaseStorageProperties {

    /** Supabase 项目 URL */
    private String url;

    /** 服务端 API Key（service_role） */
    private String serviceRoleKey;

    /** 存储桶名称 */
    private String bucket;
}
