package com.phototransform.service;

import com.phototransform.dto.AuthResponse;
import com.phototransform.dto.LoginRequest;
import com.phototransform.dto.SendCodeRequest;

/**
 * 认证服务接口
 *
 * 定义用户认证相关的业务操作：发送验证码、登录（自动注册）、登出。
 * 注册与登录合并为一个 login 接口，首次使用手机号时自动注册。
 *
 * @see com.phototransform.service.impl.AuthServiceImpl
 */
public interface AuthService {

    /**
     * 发送短信验证码
     *
     * 校验手机号格式后，生成 6 位随机验证码并模拟发送，
     * 验证码存入 Redis 并设置 5 分钟有效期。
     *
     * @param request 包含手机号的请求
     * @return 认证响应，包含成功状态和提示消息
     */
    AuthResponse sendCode(SendCodeRequest request);

    /**
     * 登录（手机号 + 验证码）
     *
     * 校验验证码后查询用户是否存在：
     * - 首次使用自动注册并创建额度（remaining=1）
     * - 已注册直接签发 JWT
     * - 被禁用账号拒绝登录
     *
     * @param request 包含手机号和验证码的请求
     * @return 认证响应，包含 token 和 userId
     */
    AuthResponse login(LoginRequest request);

    /**
     * 登出
     *
     * 将当前 token 的 jti 加入 Redis 黑名单，
     * 后续请求携带该 token 将被拒绝。
     *
     * @param token JWT 字符串
     */
    void logout(String token);
}
