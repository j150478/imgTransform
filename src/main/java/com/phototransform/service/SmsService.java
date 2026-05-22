package com.phototransform.service;

/**
 * 短信发送服务接口
 *
 * 定义短信验证码发送的通用契约。
 * 当前使用 Mock 实现模拟发送，后续可对接真实短信渠道。
 *
 * @see com.phototransform.service.impl.MockSmsService
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * 向指定的手机号发送指定内容的验证码。
     *
     * @param phone 目标手机号
     * @param code  验证码内容
     * @return true 表示发送成功，false 表示发送失败
     */
    boolean sendSms(String phone, String code);
}
