package com.phototransform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Supabase Storage 配置
 *
 * 读取 supabase.storage.* 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "supabase.storage")
public class SupabaseStorageProperties {

    /** Supabase 项目 URL */
    private String url;

    /** 服务端 API Key（service_role） */
    private String serviceRoleKey;
}
