package com.phototransform.service.impl;

/**
 * 存储服务工具类
 *
 * @author PhotoTransform Team
 * @since 1.0.0
 */
final class StorageUtils {

    private StorageUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 从文件名中提取扩展名。
     *
     * <p>如果文件名为空或不包含扩展名，默认返回 ".jpg"。</p>
     *
     * @param filename 文件名
     * @return 扩展名（包含点号，如 ".jpg"）
     */
    static String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
