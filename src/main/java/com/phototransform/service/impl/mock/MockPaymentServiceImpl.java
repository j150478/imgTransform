package com.phototransform.service.impl.mock;

import com.phototransform.common.BusinessException;
import com.phototransform.domain.entity.PaymentRecord;
import com.phototransform.dto.PaymentRequest;
import com.phototransform.dto.PaymentResponse;
import com.phototransform.enums.PayStatus;
import com.phototransform.repository.PaymentRecordRepository;
import com.phototransform.service.PaymentService;
import com.phototransform.service.QuotaService;

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
 * 校验参数 -> 创建支付记录 -> 调用 quotaService 增加额度 -> 返回结果。
 * 充值直接标记 SUCCESS，不走第三方支付回调。
 *
 * @see com.phototransform.service.PaymentService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockPaymentServiceImpl implements PaymentService {

    private final QuotaService quotaService;
    private final PaymentRecordRepository paymentRecordRepository;

    /**
     * 执行充值操作
     *
     * 步骤：
     * 1. 校验参数 - amount 必须大于 0，userId 不能为空
     * 2. 创建 PaymentRecord，payStatus 直接设为 SUCCESS，tradeNo 生成模拟流水号
     * 3. 持久化 PaymentRecord
     * 4. 增加用户额度：quotaService.increase(userId, credits) 内部使用悲观锁保证并发安全
     * 5. 构造并返回 PaymentResponse
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

        // 2. 计算获得次数（1 元 = 1 次），创建支付记录
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

        // 3. 保存支付记录
        paymentRecordRepository.save(record);
        log.info("[PAYMENT] 支付记录已创建, tradeNo: {}, amount: {}, credits: {}, userId: {}",
                tradeNo, amount, credits, userId);

        // 4. 增加用户额度（使用悲观锁保证并发安全）
        int remainingAfter = quotaService.increase(userId, credits);

        // 5. 构造并返回充值响应
        return PaymentResponse.builder()
                .tradeNo(tradeNo)
                .amount(amount)
                .credits(credits)
                .remainingAfter(remainingAfter)
                .build();
    }
}
