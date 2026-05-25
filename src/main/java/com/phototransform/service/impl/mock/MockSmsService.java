package com.phototransform.service.impl.mock;

import com.phototransform.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock 短信发送服务实现
 *
 * 在真实短信渠道对接前，通过日志输出验证码模拟发送行为。
 * 始终返回 true 表示发送成功，便于本地开发和测试。
 *
 * @see com.phototransform.service.SmsService
 */
@Slf4j
@Service
public class MockSmsService implements SmsService {

    /**
     * 模拟发送短信验证码
     *
     * 步骤：
     * 1. 通过日志输出手机号和验证码内容（不实际调用短信网关）
     * 2. 返回 true 模拟发送成功
     *
     * @param phone 目标手机号
     * @param code  验证码内容
     * @return 始终返回 true
     */
    @Override
    public boolean sendSms(String phone, String code) {
        // 1. 记录模拟发送日志
        log.info("[MOCK_SMS] 向手机号 {} 发送验证码: {}", phone, code);
        // 2. 返回发送成功
        return true;
    }
}
