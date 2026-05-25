package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.domain.entity.UserQuota;
import com.phototransform.repository.UserQuotaRepository;
import com.phototransform.service.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户额度管理实现类
 *
 * 统一管理用户额度的扣减、增加和创建操作。
 * 使用悲观锁保证并发场景下的数据一致性。
 * 仅通过 {@link QuotaService} 接口对外暴露，避免其他模块直接操作额度数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaManagerImpl implements QuotaService {

    private final UserQuotaRepository userQuotaRepository;

    /**
     * 检查并扣减用户额度
     *
     * 1. 悲观锁查询用户额度记录
     * 2. 额度账户不存在则抛出异常
     * 3. 额度不足则抛出异常
     * 4. 扣减一次额度并持久化
     *
     * @param userId 用户 ID
     * @param taskId 任务 ID（用于日志追踪）
     * @throws BusinessException 当额度账户不存在或可用次数不足时抛出
     */
    @Override
    @Transactional
    public void checkAndDeduct(Long userId, String taskId) {
        // 1. 悲观锁查询用户额度记录（SELECT FOR UPDATE）
        UserQuota quota = userQuotaRepository.findByUserIdForUpdate(userId);

        // 2. 额度账户不存在
        if (quota == null) {
            throw new BusinessException(400, "用户额度账户不存在，请联系客服");
        }

        // 3. 额度不足
        if (quota.getRemaining() <= 0) {
            throw new BusinessException(400, "可用次数不足，请充值后再试");
        }

        // 4. 扣减额度
        quota.setRemaining(quota.getRemaining() - 1);
        quota.setUpdatedTime(LocalDateTime.now());
        userQuotaRepository.save(quota);

        log.info("[QUOTA] 用户 {} 扣减一次额度, 任务: {}, 剩余: {}", userId, taskId, quota.getRemaining());
    }

    /**
     * 增加用户额度
     *
     * 1. 悲观锁查询用户额度记录
     * 2. 额度账户不存在则抛出异常
     * 3. 累加额度并持久化
     * 4. 返回累加后的剩余次数
     *
     * @param userId  用户 ID
     * @param credits 增加次数
     * @return 累加后的剩余次数
     * @throws BusinessException 当额度账户不存在时抛出
     */
    @Override
    public int increase(Long userId, int credits) {
        // 1. 悲观锁查询用户额度记录（SELECT FOR UPDATE）
        UserQuota quota = userQuotaRepository.findByUserIdForUpdate(userId);

        // 2. 额度账户不存在
        if (quota == null) {
            log.warn("[QUOTA] 用户额度账户不存在, userId: {}", userId);
            throw new BusinessException(400, "用户额度账户不存在");
        }

        // 3. 累加额度
        int before = quota.getRemaining();
        quota.setRemaining(before + credits);
        quota.setUpdatedTime(LocalDateTime.now());
        userQuotaRepository.save(quota);

        log.info("[QUOTA] 用户 {} 额度增加 {}, before: {}, after: {}", userId, credits, before, quota.getRemaining());

        // 4. 返回累加后的剩余次数
        return quota.getRemaining();
    }

    /**
     * 创建初始用户额度
     *
     * 1. 创建默认额度记录（remaining=1）
     *
     * @param userId 用户 ID
     */
    @Override
    public void create(Long userId) {
        // 1. 创建默认额度记录（remaining=1）
        LocalDateTime now = LocalDateTime.now();
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .remaining(1)
                .createdTime(now)
                .updatedTime(now)
                .build();
        userQuotaRepository.save(quota);

        log.info("[QUOTA] 新用户额度已创建, userId: {}, remaining: 1", userId);
    }
}
