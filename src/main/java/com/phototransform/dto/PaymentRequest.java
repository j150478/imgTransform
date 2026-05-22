package com.phototransform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 支付/充值请求 DTO
 *
 * 封装用户发起充值操作所需的请求参数。
 * userId 由 AuthInterceptor 通过 @RequestAttribute 注入，客户端不应携带。
 *
 * @see com.phototransform.controller.PaymentController
 * @see com.phototransform.service.PaymentService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    /**
     * 用户 ID（由服务端注入，禁止客户端伪造）
     */
    private Long userId;

    /**
     * 充值金额（单位：元），必须大于等于 0.01
     */
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于等于 0.01")
    private BigDecimal amount;

    /**
     * 支付方式（如 WECHAT / ALIPAY）
     */
    @NotBlank(message = "支付方式不能为空")
    private String payMethod;
}
