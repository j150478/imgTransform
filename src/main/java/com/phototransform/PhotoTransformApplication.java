package com.phototransform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 证件照转化服务启动类
 *
 * Spring Boot 应用主入口，负责启动整个证件照转化服务。
 * 使用 @SpringBootApplication 注解开启自动配置和组件扫描。
 *
 * 启动后提供以下功能：
 * - 接收用户上传的照片
 * - 调用图像处理大模型进行证件照生成
 * - 提供任务状态查询和结果获取接口
 *
 * 技术栈：
 * - Spring Boot 2.7.x
 * - Java 8
 * - Maven
 * - 火山引擎 Seedream 图像生成模型
 */
@SpringBootApplication
public class PhotoTransformApplication {

    /**
     * 应用主入口方法
     *
     * 启动 Spring Boot 应用，初始化 Spring 上下文和所有 Bean。
     * 执行完成后，应用开始监听配置的端口，接收 HTTP 请求。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PhotoTransformApplication.class, args);
    }
}
