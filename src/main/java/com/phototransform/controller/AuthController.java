package com.phototransform.controller;

import com.phototransform.common.ApiResponse;
import com.phototransform.dto.AuthResponse;
import com.phototransform.dto.LoginRequest;
import com.phototransform.dto.SendCodeRequest;
import com.phototransform.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证 REST API 控制器
 *
 * 提供用户认证相关的 HTTP 接口：
 * - 发送短信验证码（无需认证）
 * - 登录/自动注册（无需认证）
 * - 登出（需要 JWT 认证）
 *
 * @see com.phototransform.service.AuthService
 * @see com.phototransform.service.impl.AuthServiceImpl
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 发送短信验证码
     *
     * 步骤：
     * 1. 接收手机号参数并校验格式
     * 2. 调用 authService.sendCode() 生成并发送验证码
     * 3. 返回统一格式的成功响应
     *
     * @param request 包含手机号的请求体
     * @return 认证响应，包含 success 状态和提示消息
     */
    @PostMapping("/send-code")
    public ApiResponse<AuthResponse> sendCode(@Valid @RequestBody SendCodeRequest request) {
        log.info("[CONTROLLER] 发送验证码请求, phone: {}", request.getPhone());

        AuthResponse response = authService.sendCode(request);
        return ApiResponse.success(response);
    }

    /**
     * 登录（手机号 + 验证码）
     *
     * 步骤：
     * 1. 接收手机号和验证码参数并校验
     * 2. 调用 authService.login() 校验验证码、自动注册或签发 JWT
     * 3. 返回包含 token 和 userId 的统一响应
     *
     * @param request 包含手机号和验证码的请求体
     * @return 认证响应，成功时包含 token 和 userId
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("[CONTROLLER] 登录请求, phone: {}", request.getPhone());

        AuthResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    /**
     * 登出
     *
     * 步骤：
     * 1. 从 Authorization 头中提取 Bearer token
     * 2. 调用 authService.logout() 将 token 加入黑名单
     * 3. 返回登出成功的统一响应
     *
     * @param authHeader Authorization 请求头（格式：Bearer xxx）
     * @return 登出成功消息
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout(@RequestHeader("Authorization") String authHeader) {
        log.info("[CONTROLLER] 登出请求");

        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ApiResponse.success("已登出");
    }
}
