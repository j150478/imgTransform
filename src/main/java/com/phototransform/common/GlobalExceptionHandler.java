package com.phototransform.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * <p>
     * 当业务逻辑执行过程中抛出 {@link BusinessException} 时，
     * 将其转换为统一的 API 错误响应返回给客户端。
     * HTTP 状态码保持 200，业务错误码由 BusinessException 提供。
     *
     * @param e 业务异常
     * @return 包含错误码和错误消息的统一响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid）
     * <p>
     * 当控制器方法参数使用 {@link javax.validation.Valid @Valid} 注解校验失败时，
     * 收集所有字段的校验失败消息，以分号拼接后返回。
     *
     * @param e 参数校验异常
     * @return 包含 400 错误码和校验失败消息的统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ApiResponse.error(400, message);
    }

    /**
     * 处理约束违反异常（@Validated）
     * <p>
     * 当控制器类级别使用 {@link org.springframework.validation.annotation.Validated @Validated} 注解
     * 且参数校验失败时，收集所有约束违反消息，以分号拼接后返回。
     *
     * @param e 约束违反异常
     * @return 包含 400 错误码和约束违反消息的统一响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数约束违反: {}", message);
        return ApiResponse.error(400, message);
    }

    /**
     * 处理缺少请求参数异常
     * <p>
     * 当请求缺少必需的参数（如 {@link org.springframework.web.bind.annotation.RequestParam @RequestParam}
     * 设置了 required=true）时，返回缺少的参数名称。
     *
     * @param e 缺少请求参数异常
     * @return 包含 400 错误码和缺少参数名称的统一响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleMissingParamException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return ApiResponse.error(400, "缺少请求参数: " + e.getParameterName());
    }

    /**
     * 处理文件上传大小超限异常
     * <p>
     * 当上传的文件大小超过 spring.servlet.multipart.max-file-size 或
     * spring.servlet.multipart.max-request-size 配置的最大限制时，
     * 返回统一的文件大小超限提示。
     *
     * @param e 文件上传大小超限异常
     * @return 包含 400 错误码和大小超限消息的统一响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件大小超限: {}", e.getMessage());
        return ApiResponse.error(400, "上传文件大小超过限制");
    }

    /**
     * 处理其他未知异常
     * <p>
     * 作为兜底异常处理器，捕获所有未被上述特定处理器处理的未预期异常。
     * 返回 500 服务器内部错误，并记录完整异常堆栈用于排查。
     *
     * @param e 未知异常
     * @return 包含 500 错误码的通用错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("未知异常", e);
        return ApiResponse.error(500, "服务器内部错误");
    }
}
