package com.phototransform.repository;

import com.phototransform.domain.entity.UserQuota;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;

/**
 * 用户额度数据访问接口
 *
 * 基于 Spring Data JPA，提供 UserQuota 实体的基础 CRUD 操作。
 * 包含悲观锁查询方法，用于并发安全的额度扣减。
 */
public interface UserQuotaRepository extends JpaRepository<UserQuota, Long> {

    /**
     * 根据用户 ID 查询额度记录
     *
     * @param userId 用户 ID
     * @return 匹配的额度记录，不存在时返回 null
     */
    UserQuota findByUserId(Long userId);

    /**
     * 根据用户 ID 悲观锁查询额度记录
     *
     * 使用 SELECT FOR UPDATE 锁定额度行，保证并发场景下额度扣减的安全性。
     * 需要在 {@link org.springframework.transaction.annotation.Transactional} 上下文中调用。
     *
     * @param userId 用户 ID
     * @return 匹配的额度记录，不存在时返回 null
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM UserQuota q WHERE q.userId = :userId")
    UserQuota findByUserIdForUpdate(@Param("userId") Long userId);
}
