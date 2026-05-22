package com.phototransform.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 认证拦截器，校验请求中的 JWT Token。
 *
 * <p>从请求头 {@code Authorization} 中提取 Bearer Token，
 * 校验签名和有效期，并检查 Redis 黑名单。校验通过后将 userId
 * 写入 request attribute 供后续处理器使用。</p>
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** Authorization 请求头名称 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer Token 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** Redis 黑名单键前缀 */
    private static final String BLACKLIST_KEY_PREFIX = "jwt:black:";

    private final RedisTemplate<String, String> redisTemplate;

    private final JwtUtil jwtUtil;

    /**
     * 构造认证拦截器。
     *
     * @param redisTemplate Redis 操作模板，用于黑名单查询
     * @param jwtUtil       JWT 工具类，用于 Token 的解析和校验
     */
    public AuthInterceptor(RedisTemplate<String, String> redisTemplate, JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 请求预处理：校验 JWT Token。
     *
     * <p>处理步骤：
     * <ol>
     *   <li>从请求头获取 Authorization 值</li>
     *   <li>去除 "Bearer " 前缀获取 token</li>
     *   <li>token 为空时抛出 401 异常</li>
     *   <li>校验 token 签名和有效期</li>
     *   <li>校验失败抛出 401 异常</li>
     *   <li>查询 Redis 黑名单，命中则抛出 401 异常</li>
     *   <li>解析 userId 写入 request attribute</li>
     *   <li>返回 true 放行</li>
     * </ol></p>
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler  当前处理器
     * @return 始终返回 true（校验失败由 {@link BusinessException} 处理）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头获取 Authorization 值
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // 2. 去除 "Bearer " 前缀获取 token
        String token = null;
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        }

        // 3. token 为空时抛出 401 异常
        if (token == null || token.isEmpty()) {
            log.warn("请求缺少有效的 Authorization 头");
            throw new BusinessException(401, "未登录");
        }

        // 4. 校验 token 签名和有效期
        if (!jwtUtil.validateToken(token)) {
            // 5. 校验失败抛出 401 异常
            log.warn("无效或已过期的 token: {}...", token.substring(0, Math.min(20, token.length())));
            throw new BusinessException(401, "token无效或已过期");
        }

        // 6. 查询 Redis 黑名单
        String jti = jwtUtil.getJtiFromToken(token);
        String blacklistKey = BLACKLIST_KEY_PREFIX + jti;
        Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            log.warn("已被登出的 token, jti: {}", jti);
            throw new BusinessException(401, "token已被登出");
        }

        // 7. 解析 userId 写入 request attribute
        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);

        // 8. 返回 true 放行
        log.debug("token 校验通过, userId: {}", userId);
        return true;
    }
}
