package com.phototransform.repository;

import com.phototransform.domain.entity.PaymentRecord;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 支付记录数据访问接口
 *
 * 基于 Spring Data JPA，提供 PaymentRecord 实体的基础 CRUD 操作。
 */
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
}
