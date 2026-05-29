package com.phototransform.domain.entity;

import com.phototransform.enums.ImageTaskStatus;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图像生成任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "image_generation_task")
public class ImageGenerationTask {

    @Id
    private String taskId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 2000)
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageTaskStatus status;

    @Column(length = 4000)
    private String resultImageUrls;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdTime;

    @Column(nullable = false)
    private LocalDateTime updatedTime;
}
