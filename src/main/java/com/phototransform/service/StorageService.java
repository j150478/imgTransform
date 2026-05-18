package com.phototransform.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 本地文件存储服务接口
 */
public interface StorageService {

    /**
     * 存储上传文件
     *
     * @param file 上传的文件
     * @param taskId 任务ID，用于生成唯一文件名
     * @return 文件的访问 URL
     */
    String store(MultipartFile file, String taskId);

    /**
     * 存储字节数据
     *
     * @param data 图片字节数据
     * @param fileName 文件名
     * @return 文件的访问 URL
     */
    String store(byte[] data, String fileName);

    /**
     * 检查文件是否存在
     *
     * @param fileName 文件名
     * @return 是否存在
     */
    boolean exists(String fileName);

    /**
     * 根据存储 URL 读取文件字节数据
     *
     * @param url 文件的访问 URL（由 store 方法返回）
     * @return 文件字节数据
     */
    byte[] readByUrl(String url);
}
