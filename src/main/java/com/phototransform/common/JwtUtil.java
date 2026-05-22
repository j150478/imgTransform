package com.phototransform.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类，提供 Token 的生成、解析与校验功能。
 *
 * <p>从 {@code jwt.secret} 读取签名密钥，从 {@code jwt.expiration-days} 读取过期天数。
 * 使用 jjwt 0.11.5 库实现 HS256 签名的 JWT Token 管理。</p>
 */
@Component
public class JwtUtil {

    /** 签名密钥 */
    private final SecretKey secretKey;

    /** Token 过期时间（毫秒） */
    private final long expirationMillis;

    /**
     * 构造 JWT 工具类，从配置文件注入密钥和过期天数。
     *
     * @param secret         签名密钥字符串，对应 jwt.secret 配置项
     * @param expirationDays 过期天数，对应 jwt.expiration-days 配置项
     */
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-days}") long expirationDays) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationDays * 24 * 60 * 60 * 1000L;
    }

    /**
     * 生成 JWT Token。
     *
     * <p>Payload 中包含 userId、jti（UUID）、签发时间和过期时间。
     * 使用 HS256 算法签名。</p>
     *
     * @param userId 用户 ID
     * @return 生成的 JWT 字符串
     */
    public String generateToken(Long userId) {
        // 1. 生成唯一 JWT ID
        String jti = UUID.randomUUID().toString();

        // 2. 计算签发时间和过期时间
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);

        // 3. 构建并签名 JWT
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .setId(jti)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 Token 中解析用户 ID。
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        // 1. 解析 JWT 获取 Claims
        Claims claims = parseClaims(token);
        // 2. 从 subject 中获取 userId
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 Token 中解析 JWT ID（jti）。
     *
     * @param token JWT 字符串
     * @return JWT ID
     */
    public String getJtiFromToken(String token) {
        // 1. 解析 JWT 获取 Claims
        Claims claims = parseClaims(token);
        // 2. 返回 JWT ID
        return claims.getId();
    }

    /**
     * 从 Token 中解析过期时间。
     *
     * @param token JWT 字符串
     * @return 过期时间
     */
    public Date getExpirationFromToken(String token) {
        // 1. 解析 JWT 获取 Claims
        Claims claims = parseClaims(token);
        // 2. 返回过期时间
        return claims.getExpiration();
    }

    /**
     * 校验 Token 的签名和有效期。
     *
     * <p>解析成功且未过期返回 true，签名错误或已过期返回 false。</p>
     *
     * @param token JWT 字符串
     * @return true 表示 token 有效，false 表示无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            // 1. 解析 JWT（签名校验和过期校验由 jjwt 自动完成）
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 2. 解析失败返回 false
            return false;
        }
    }

    /**
     * 解析 JWT Token 的 Claims 部分。
     *
     * @param token JWT 字符串
     * @return Claims 对象
     */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
