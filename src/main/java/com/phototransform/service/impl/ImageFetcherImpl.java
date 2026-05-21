package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.service.ImageFetcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.net.URL;

/**
 * 图片抓取服务实现，从远程 URL 下载图片字节数据
 */
@Slf4j
@Component
public class ImageFetcherImpl implements ImageFetcher {

    /**
     * 从 URL 下载图片字节数据
     */
    @Override
    public byte[] fetch(String url) {
        // 1. 创建 URL 对象并打开输入流
        try {
            URL imageUrl = new URL(url);
            try (InputStream is = imageUrl.openStream()) {
                // 2. 读取完整字节数据
                byte[] data = StreamUtils.copyToByteArray(is);
                log.debug("从 {} 下载图片完成, 大小: {} bytes", url, data.length);
                return data;
            }
        } catch (Exception e) {
            // 3. 异常时包装为业务异常
            throw new BusinessException(500, "保存结果图片失败: " + e.getMessage(), e);
        }
    }
}
