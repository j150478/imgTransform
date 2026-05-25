package com.phototransform.service;

/**
 * 用户额度服务接口
 *
 * 定义用户额度检查与扣减、额度增加、初始额度创建的核心操作契约。
 * checkAndDeduct 在任务创建流程中调用，addCredits 在充值流程中调用，
 * create 在新用户注册时调用。
 * 所有修改操作均使用悲观锁（SELECT FOR UPDATE）保证并发安全。
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

    /**
     * 为用户额度增加 credits 次
     *
     * 使用悲观锁（SELECT FOR UPDATE）查询用户额度记录。
     * 在充值流程中调用，确保并发充值场景下额度累加的准确性。
     * 该方法不单独开启事务，由调用方负责事务边界管理。
     *
     * @param userId  用户 ID
     * @param credits 增加的次数
     * @return 增加后的剩余次数
     * @throws com.phototransform.common.BusinessException 当额度账户不存在时抛出
     */
    int addCredits(Long userId, int credits);

    /**
     * 创建初始用户额度
     *
     * 为新注册用户创建默认额度记录（remaining=1）。
     * 在用户登录流程中，检测到新用户时调用。
     *
     * @param userId 用户 ID
     */
    void create(Long userId);
}
