package com.phototransform.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.phototransform.enums.PayStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付记录实体
 *
 * 映射数据库 payment_record 表，记录用户支付信息。
 * 每次支付新增一条记录，关联用户和获得的次数额度。
 *
 * @see com.phototransform.enums.PayStatus
 * @see com.phototransform.repository.PaymentRecordRepository
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_record")
public class PaymentRecord {

    /**
     * 支付记录 ID（主键，自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户 ID（关联 User.id）
     */
    @Column
    private Long userId;

    /**
     * 支付金额（单位：元）
     */
    @Column
    private BigDecimal amount;

    /**
     * 获得的使用次数
     */
    @Column
    private Integer credits;

    /**
     * 支付状态（默认 PENDING）
     *
     * @see PayStatus
     */
    @Enumerated(EnumType.STRING)
    @Column
    private PayStatus payStatus = PayStatus.PENDING;

    /**
     * 支付方式（WECHAT / ALIPAY）
     */
    @Column
    private String payMethod;

    /**
     * 第三方交易流水号
     */
    @Column
    private String tradeNo;

    /**
     * 创建时间
     */
    @Column
    private LocalDateTime createdTime;
}
