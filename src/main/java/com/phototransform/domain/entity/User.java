package com.phototransform.domain.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.phototransform.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户账号实体
 *
 * 映射数据库 user_account 表，记录用户账号信息。
 * 每个用户通过手机号唯一标识，关联额度记录和支付记录。
 *
 * @see com.phototransform.enums.UserStatus
 * @see com.phototransform.repository.UserRepository
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_account")
public class User {

    /**
     * 用户 ID（主键，自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 手机号（唯一索引）
     */
    @Column(unique = true)
    private String phone;

    /**
     * 用户状态（默认 ACTIVE）
     *
     * @see UserStatus
     */
    @Enumerated(EnumType.STRING)
    @Column
    private UserStatus status = UserStatus.ACTIVE;

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
