package com.phototransform.domain.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户额度实体
 *
 * 映射数据库 user_quota 表，记录用户剩余可使用次数。
 * 每个用户唯一一条额度记录，通过 userId 关联 User 实体。
 *
 * @see com.phototransform.domain.entity.User
 * @see com.phototransform.repository.UserQuotaRepository
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_quota")
public class UserQuota {

    /**
     * 额度记录 ID（主键，自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户 ID（唯一，关联 User.id）
     */
    @Column(unique = true)
    private Long userId;

    /**
     * 剩余使用次数（默认 1）
     */
    @Column
    private Integer remaining = 1;

    /**
     * 创建时间
     */
    @Column
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @Column
    private LocalDateTime updatedTime;
}
