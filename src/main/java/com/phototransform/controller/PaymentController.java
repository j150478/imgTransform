package com.phototransform.controller;

import com.phototransform.common.ApiResponse;
import com.phototransform.dto.PaymentRequest;
import com.phototransform.dto.PaymentResponse;
import com.phototransform.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 支付 REST API 控制器
 *
 * 提供充值相关的 HTTP 接口。
 * 当前仅支持模拟充值，实际支付 SDK 尚未接入。
 * userId 通过 AuthInterceptor 从请求上下文中注入，防止客户端伪造。
 *
 * @see com.phototransform.service.PaymentService
 * @see com.phototransform.service.impl.MockPaymentServiceImpl
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 执行充值操作
     *
     * 步骤：
     * 1. 从 AuthInterceptor 获取当前登录用户 ID（@RequestAttribute("userId")）
     * 2. 将用户 ID 注入到请求参数中（覆盖客户端传入的值，防止伪造）
     * 3. 调用 paymentService.recharge() 执行业务逻辑
     * 4. 返回统一格式的成功响应
     *
     * @param userId  当前登录用户 ID（由 AuthInterceptor 注入）
     * @param request 充值请求体（含金额和支付方式）
     * @return 充值响应，包含流水号、金额、获得次数和剩余次数
     */
    @PostMapping("/recharge")
    public ApiResponse<PaymentResponse> recharge(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PaymentRequest request) {
        log.info("[CONTROLLER] 接收到充值请求, userId: {}, amount: {}, payMethod: {}",
                userId, request.getAmount(), request.getPayMethod());

        // 用 interceptor 传入的 userId，防止伪造
        request.setUserId(userId);
        PaymentResponse response = paymentService.recharge(request);
        return ApiResponse.success(response);
    }
}
