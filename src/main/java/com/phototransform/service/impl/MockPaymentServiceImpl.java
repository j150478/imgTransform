package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.domain.entity.PaymentRecord;
import com.phototransform.domain.entity.UserQuota;
import com.phototransform.dto.PaymentRequest;
import com.phototransform.dto.PaymentResponse;
import com.phototransform.enums.PayStatus;
import com.phototransform.repository.PaymentRecordRepository;
import com.phototransform.repository.UserQuotaRepository;
import com.phototransform.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mock 支付服务实现
 *
 * 在真实支付 SDK 对接前，模拟充值流程：
 * 校验参数 -> 查询额度 -> 创建支付记录 -> 累加额度 -> 返回结果。
 * 充值直接标记 SUCCESS，不走第三方支付回调。
 *
 * @see com.phototransform.service.PaymentService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockPaymentServiceImpl implements PaymentService {

    private final UserQuotaRepository userQuotaRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    /**
     * 执行充值操作
     *
     * 步骤：
     * 1. 校验参数 - amount 必须大于 0，userId 不能为空
     * 2. 根据 userId 查询用户额度记录（UserQuota）
     * 3. 额度记录不存在时抛 BusinessException(400, "用户额度账户不存在")
     * 4. 创建 PaymentRecord，payStatus 直接设为 SUCCESS，tradeNo 生成模拟流水号
     * 5. 持久化 PaymentRecord
     * 6. 累加额度：quota.remaining += credits（1 元 = 1 次），充值无并发扣减问题，使用普通 update
     * 7. 更新 quota.updatedTime 为当前时间，保存 quota
     * 8. 构造并返回 PaymentResponse
     *
     * @param request 充值请求，包含 userId、amount、payMethod
     * @return 充值响应，包含流水号、金额、获得次数和剩余次数
     * @throws BusinessException 参数校验失败或额度账户不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse recharge(PaymentRequest request) {
        // 1. 校验参数
        BigDecimal amount = request.getAmount();
        Long userId = request.getUserId();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[PAYMENT] 充值金额无效: {}", amount);
            throw new BusinessException(400, "充值金额必须大于 0");
        }
        if (userId == null) {
            log.warn("[PAYMENT] 用户 ID 不能为空");
            throw new BusinessException(400, "用户 ID 不能为空");
        }

        // 2. 查询用户额度记录
        UserQuota quota = userQuotaRepository.findByUserId(userId);

        // 3. 额度记录不存在时抛出异常
        if (quota == null) {
            log.warn("[PAYMENT] 用户额度账户不存在, userId: {}", userId);
            throw new BusinessException(400, "用户额度账户不存在");
        }

        // 4. 计算获得次数（1 元 = 1 次），创建支付记录
        int credits = amount.intValue();
        String tradeNo = "MOCK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        PaymentRecord record = PaymentRecord.builder()
                .userId(userId)
                .amount(amount)
                .credits(credits)
                .payStatus(PayStatus.SUCCESS)
                .payMethod(request.getPayMethod())
                .tradeNo(tradeNo)
                .createdTime(now)
                .build();

        // 5. 保存支付记录
        paymentRecordRepository.save(record);
        log.info("[PAYMENT] 支付记录已创建, tradeNo: {}, amount: {}, credits: {}, userId: {}",
                tradeNo, amount, credits, userId);

        // 6. 累加额度
        int remainingBefore = quota.getRemaining();
        quota.setRemaining(remainingBefore + credits);

        // 7. 更新更新时间，保存额度
        quota.setUpdatedTime(now);
        userQuotaRepository.save(quota);
        log.info("[PAYMENT] 额度已更新, userId: {}, before: {}, after: {}",
                userId, remainingBefore, quota.getRemaining());

        // 8. 构造并返回充值响应
        return PaymentResponse.builder()
                .tradeNo(tradeNo)
                .amount(amount)
                .credits(credits)
                .remainingAfter(quota.getRemaining())
                .build();
    }
}
