package com.phototransform.common;

/**
 * 业务异常类
 * 
 * 用于封装业务逻辑执行过程中发生的异常情况。
 * 提供错误码和错误消息，便于统一异常处理和错误响应。
 * 
 * 使用示例：
 * throw new BusinessException(400, "请求参数错误");
 * throw new BusinessException(404, "任务不存在");
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     * 用于标识具体的错误类型，便于客户端处理和国际化
     * 常见错误码：
     * 400 - 请求参数错误
     * 401 - 未授权
     * 403 - 禁止访问
     * 404 - 资源不存在
     * 409 - 资源冲突
     * 500 - 服务器内部错误
     */
    private final Integer code;

    /**
     * 构造业务异常
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造业务异常（带原因）
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public Integer getCode() {
        return code;
    }
}
