package com.phototransform.service;

/**
 * 图片抓取服务，从远程 URL 下载图片字节数据
 */
public interface ImageFetcher {

    /**
     * 从 URL 下载图片字节数据
     *
     * @param url 图片的远程 URL
     * @return 图片字节数组
     * @throws com.phototransform.common.BusinessException 下载失败时抛出
     */
    byte[] fetch(String url);
}
