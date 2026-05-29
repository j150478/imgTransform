package com.phototransform.repository;

import com.phototransform.domain.entity.ImageGenerationTask;
import com.phototransform.enums.ImageTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageGenerationTaskRepository extends JpaRepository<ImageGenerationTask, String> {

    List<ImageGenerationTask> findByStatusAndCreatedTimeBefore(ImageTaskStatus status, java.time.LocalDateTime threshold);
}
