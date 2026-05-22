package com.phototransform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 认证响应 DTO
 *
 * 封装认证操作的返回结果。
 * 发送验证码时仅返回 success 和 message；
 * 登录成功时额外返回 token 和 userId。
 *
 * @see com.phototransform.controller.AuthController
 * @see com.phototransform.service.AuthService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 提示消息
     */
    private String message;

    /**
     * JWT token（登录成功时返回）
     */
    private String token;

    /**
     * 用户 ID（登录成功时返回）
     */
    private Long userId;
}
