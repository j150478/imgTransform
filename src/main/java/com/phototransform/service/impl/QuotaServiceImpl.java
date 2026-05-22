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
 * 用户额度服务实现类
 *
 * 在证件照转化任务创建流程中执行额度检查与扣减。
 * 使用悲观锁保证并发场景下的额度扣减准确性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

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
}
