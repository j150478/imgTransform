package com.phototransform.service;

/**
 * 用户额度服务接口
 *
 * 定义用户额度检查与扣减的核心操作契约。
 * 在任务创建流程中调用，确保用户有足够额度执行证件照转化操作。
 */
public interface QuotaService {

    /**
     * 检查并扣减用户额度
     *
     * 使用悲观锁（SELECT FOR UPDATE）查询用户额度记录。
     * 若额度不存在或余额不足则抛出 BusinessException。
     * 扣减操作与查询在同一个事务中完成，保证并发安全。
     *
     * @param userId 用户 ID
     * @param taskId 任务 ID（用于日志追踪）
     * @throws com.phototransform.common.BusinessException 当额度不存在或不足时抛出
     */
    void checkAndDeduct(Long userId, String taskId);
}
