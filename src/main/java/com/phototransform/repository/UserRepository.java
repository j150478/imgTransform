package com.phototransform.repository;

import com.phototransform.domain.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户账号数据访问接口
 *
 * 基于 Spring Data JPA，提供 User 实体的基础 CRUD 操作。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 匹配的用户，不存在时返回 null
     */
    User findByPhone(String phone);
}
