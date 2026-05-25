package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.common.JwtUtil;
import com.phototransform.domain.entity.User;
import com.phototransform.dto.AuthResponse;
import com.phototransform.dto.LoginRequest;
import com.phototransform.dto.SendCodeRequest;
import com.phototransform.enums.UserStatus;
import com.phototransform.repository.UserRepository;
import com.phototransform.service.AuthService;
import com.phototransform.service.QuotaService;
import com.phototransform.service.SmsService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现
 *
 * 提供短信验证码发送、手机号登录（自动注册）和 JWT 登出功能。
 * 验证码使用 Redis 存储，支持 5 分钟有效期和黑名单机制。
 *
 * @see com.phototransform.service.AuthService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 手机号正则表达式：1 开头，第二位 3-9，共 11 位 */
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /** Redis 验证码键前缀 */
    private static final String SMS_KEY_PREFIX = "sms:";

    /** Redis 黑名单键前缀 */
    private static final String BLACKLIST_KEY_PREFIX = "jwt:black:";

    /** 验证码有效期（分钟） */
    private static final long CODE_TTL_MINUTES = 5;

    /** 验证码长度 */
    private static final int CODE_LENGTH = 6;

    private final UserRepository userRepository;
    private final QuotaService quotaService;
    private final JwtUtil jwtUtil;
    private final SmsService smsService;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 发送短信验证码
     *
     * 步骤：
     * 1. 校验手机号格式（正则 ^1[3-9]\\d{9}$），不合法抛 BusinessException(400, "手机号格式不正确")
     * 2. 生成 6 位随机数字验证码
     * 3. 调用 smsService.sendSms(phone, code) 模拟发送
     * 4. 验证码存入 Redis，key=sms:{phone}，value=code，TTL=5 分钟
     * 5. 返回 AuthResponse（success=true, message="验证码已发送"）
     *
     * @param request 包含手机号的请求
     * @return 认证响应，包含成功状态和提示消息
     * @throws BusinessException 手机号格式不合法时抛出
     */
    @Override
    public AuthResponse sendCode(SendCodeRequest request) {
        String phone = request.getPhone();

        // 1. 校验手机号格式
        if (phone == null || !phone.matches(PHONE_REGEX)) {
            log.warn("[AUTH] 手机号格式不正确: {}", phone);
            throw new BusinessException(400, "手机号格式不正确");
        }

        // 2. 生成 6 位随机数字验证码
        String code = generateRandomCode();
        log.info("[AUTH] 生成验证码, phone: {}, code: {}", phone, code);

        // 3. 模拟发送短信
        smsService.sendSms(phone, code);

        // 4. 验证码存入 Redis，5 分钟过期
        String redisKey = SMS_KEY_PREFIX + phone;
        redisTemplate
            .opsForValue()
            .set(redisKey, code, Duration.ofMinutes(CODE_TTL_MINUTES));
        log.info(
            "[AUTH] 验证码已存入 Redis, key: {}, TTL: {} 分钟",
            redisKey,
            CODE_TTL_MINUTES
        );

        // 5. 返回成功响应
        return AuthResponse.builder()
            .success(true)
            .message("验证码已发送")
            .build();
    }

    /**
     * 登录（手机号 + 验证码）
     *
     * 步骤：
     * 1. 校验手机号格式
     * 2. 校验验证码不为空
     * 3. 从 Redis 查询验证码，key=sms:{phone}
     * 4. 验证码不存在（已过期），抛 BusinessException(400, "验证码已过期，请重新获取")
     * 5. 验证码不匹配，抛 BusinessException(400, "验证码错误")
     * 6. 验证通过后删除 Redis 中的验证码
     * 7. 根据手机号查询用户
     * 8. 用户不存在，创建新用户（status=ACTIVE）并通过 quotaService 创建初始额度
     * 9. 用户存在但 status=INACTIVE，抛 BusinessException(403, "账号已被禁用")
     * 10. 生成 JWT token
     * 11. 返回 AuthResponse（success=true, token=token, userId=user.getId()）
     *
     * @param request 包含手机号和验证码的请求
     * @return 认证响应，包含 token 和 userId
     * @throws BusinessException 参数校验失败、验证码错误或账号被禁用时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse login(LoginRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();

        // 1. 校验手机号格式
        if (phone == null || !phone.matches(PHONE_REGEX)) {
            log.warn("[AUTH] 登录手机号格式不正确: {}", phone);
            throw new BusinessException(400, "手机号格式不正确");
        }

        // 2. 校验验证码不为空
        if (code == null || code.trim().isEmpty()) {
            log.warn("[AUTH] 验证码为空, phone: {}", phone);
            throw new BusinessException(400, "验证码不能为空");
        }

        // 3. 从 Redis 查询验证码
        String redisKey = SMS_KEY_PREFIX + phone;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        // 4. 验证码不存在（已过期）
        if (storedCode == null) {
            log.warn("[AUTH] 验证码已过期, phone: {}", phone);
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }

        // 5. 验证码不匹配
        if (!storedCode.equals(code)) {
            log.warn("[AUTH] 验证码错误, phone: {}", phone);
            throw new BusinessException(400, "验证码错误");
        }

        // 6. 验证通过，删除 Redis 验证码
        redisTemplate.delete(redisKey);
        log.info("[AUTH] 验证码校验通过, phone: {}", phone);

        // 7. 根据手机号查询用户
        User user = userRepository.findByPhone(phone);

        // 8. 用户不存在，自动注册
        if (user == null) {
            LocalDateTime now = LocalDateTime.now();
            user = User.builder()
                .phone(phone)
                .status(UserStatus.ACTIVE)
                .createdTime(now)
                .updatedTime(now)
                .build();
            user = userRepository.save(user);
            log.info(
                "[AUTH] 新用户注册成功, userId: {}, phone: {}",
                user.getId(),
                phone
            );

            // 创建默认额度记录（remaining=1）
            quotaService.create(user.getId());
        } else {
            // 9. 用户存在但被禁用
            if (UserStatus.INACTIVE.equals(user.getStatus())) {
                log.warn(
                    "[AUTH] 账号已被禁用, userId: {}, phone: {}",
                    user.getId(),
                    phone
                );
                throw new BusinessException(403, "账号已被禁用");
            }
            log.info(
                "[AUTH] 用户已存在, userId: {}, phone: {}",
                user.getId(),
                phone
            );
        }

        // 10. 生成 JWT token
        String token = jwtUtil.generateToken(user.getId());
        log.info("[AUTH] JWT 已生成, userId: {}", user.getId());

        // 11. 返回登录成功响应
        return AuthResponse.builder()
            .success(true)
            .message("登录成功")
            .token(token)
            .userId(user.getId())
            .build();
    }

    /**
     * 登出
     *
     * 步骤：
     * 1. 从 token 中解析 jti 和过期时间
     * 2. 计算剩余有效时间（秒）：expiration - now
     * 3. 剩余时间大于 0，将 jti 加入 Redis 黑名单，TTL 为剩余有效时间
     * 4. 剩余时间小于等于 0，token 已过期，直接返回
     *
     * @param token JWT 字符串
     */
    @Override
    public void logout(String token) {
        // 1. 从 token 中解析 jti 和过期时间
        String jti = jwtUtil.getJtiFromToken(token);
        Date expiration = jwtUtil.getExpirationFromToken(token);

        // 2. 计算剩余有效时间（毫秒）
        long now = System.currentTimeMillis();
        long ttlMillis = expiration.getTime() - now;

        // 3. 剩余时间大于 0，加入黑名单
        if (ttlMillis > 0) {
            String blacklistKey = BLACKLIST_KEY_PREFIX + jti;
            redisTemplate
                .opsForValue()
                .set(blacklistKey, "1", Duration.ofMillis(ttlMillis));
            log.info(
                "[AUTH] token 已加入黑名单, jti: {}, TTL: {} 毫秒",
                jti,
                ttlMillis
            );
        } else {
            // 4. token 已过期，直接返回
            log.info("[AUTH] token 已过期，无需加入黑名单, jti: {}", jti);
        }
    }

    /**
     * 生成指定长度的随机数字验证码
     *
     * @return 随机数字字符串
     */
    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
