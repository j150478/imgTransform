package com.phototransform.enums;

/**
 * 支付状态枚举
 *
 * 标识支付记录的生命周期状态：
 * PENDING - 支付处理中（未完成）
 * SUCCESS - 支付成功
 * FAILED  - 支付失败
 */
public enum PayStatus {

    /**
     * 处理中 - 支付请求已发起，等待支付结果
     */
    PENDING,

    /**
     * 成功 - 支付已完成
     */
    SUCCESS,

    /**
     * 失败 - 支付失败或取消
     */
    FAILED
}
