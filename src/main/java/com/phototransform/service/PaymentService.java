package com.phototransform.service;

import com.phototransform.dto.PaymentRequest;
import com.phototransform.dto.PaymentResponse;

/**
 * 支付服务接口
 *
 * 定义充值相关的业务操作。
 * 当前为 Mock 实现，尚无真实支付 SDK 对接。
 *
 * @see com.phototransform.service.impl.mock.MockPaymentServiceImpl
 */
public interface PaymentService {

    /**
     * 执行充值操作
     *
     * 模拟第三方支付回调，直接标记支付成功。
     * 将充值金额按 1 元 = 1 次的比例转换为使用次数，累加到用户额度中。
     *
     * @param request 充值请求，包含用户 ID、金额和支付方式
     * @return 充值响应，包含流水号、金额、获得次数和剩余次数
     */
    PaymentResponse recharge(PaymentRequest request);
}
