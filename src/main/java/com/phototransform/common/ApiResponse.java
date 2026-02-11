package com.phototransform.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应对象
 * 
 * 用于包装所有接口的响应数据，提供统一的响应格式。
 * 包含响应状态码、消息说明和实际业务数据。
 * 
 * 响应格式示例：
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { ... }
 * }
 *
 * @param <T> 业务数据类型
 */
@Data
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     * 200 - 成功
     * 400 - 请求参数错误
     * 404 - 资源不存在
     * 500 - 服务器内部错误
     */
    private Integer code;

    /**
     * 响应消息说明
     * 成功时为 "success"，失败时为具体错误描述
     */
    private String message;

    /**
     * 业务数据
     * 成功时包含实际的业务响应对象，失败时可能为 null
     */
    private T data;

    /**
     * 私有构造方法，强制使用静态工厂方法创建实例
     */
    private ApiResponse() {
    }

    /**
     * 创建成功响应
     * 
     * 用于业务处理成功时返回响应数据。
     * 默认状态码为 200，消息为 "success"。
     * 
     * @param data 业务数据
     * @param <T> 数据类型
     * @return 成功的 API 响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    /**
     * 创建成功响应（无数据）
     * 
     * 用于业务处理成功但无需返回数据时。
     * 
     * @param <T> 数据类型
     * @return 成功的 API 响应对象
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 创建失败响应
     * 
     * 用于业务处理失败时返回错误信息。
     * 需要指定错误码和错误消息。
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 失败的 API 响应对象
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(null);
        return response;
    }
}
