package com.phototransform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 支付/充值响应 DTO
 *
 * 封装充值操作完成后返回给客户端的响应数据，
 * 包含模拟流水号、充值金额、获得次数和充值后剩余次数。
 *
 * @see com.phototransform.controller.PaymentController
 * @see com.phototransform.service.PaymentService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /**
     * 模拟交易流水号（格式：MOCK_ + 16 位大写字母数字）
     */
    private String tradeNo;

    /**
     * 充值金额（单位：元）
     */
    private BigDecimal amount;

    /**
     * 获得的使用次数（1 元 = 1 次）
     */
    private Integer credits;

    /**
     * 充值后剩余使用次数
     */
    private Integer remainingAfter;
}
