package com.phototransform.controller;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.phototransform.common.ApiResponse;
import com.phototransform.dto.PhotoTransformRequest;
import com.phototransform.dto.PhotoTransformResponse;
import com.phototransform.dto.PhotoTransformResultResponse;
import com.phototransform.service.PhotoTransformService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 证件照转化 REST API 控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/photo")
@RequiredArgsConstructor
public class PhotoTransformController {

    private final PhotoTransformService photoTransformService;

    /**
     * 提交证件照转化任务
     *
     * @param request 转化请求参数
     * @param userId  用户 ID（由 AuthInterceptor 写入请求属性）
     * @return 任务ID和状态
     */
    @PostMapping("/transform")
    public ApiResponse<PhotoTransformResponse> transform(
            @Valid @ModelAttribute PhotoTransformRequest request,
            @RequestAttribute("userId") Long userId) {
        log.info("[CONTROLLER] 用户 {} 提交证件照转化请求, modelType: {}, backgroundColor: {}, photoType: {}, fileName: {}, fileSize: {} bytes",
                userId, request.getModelType(), request.getBackgroundColor(), request.getPhotoType(),
                request.getFile() != null ? request.getFile().getOriginalFilename() : "null",
                request.getFile() != null ? request.getFile().getSize() : 0L);
        PhotoTransformResponse response = photoTransformService.createTransformTask(request, userId);
        return ApiResponse.success(response);
    }

    /**
     * 查询证件照转化结果
     *
     * @param taskId 任务唯一标识
     * @return 任务状态和结果
     */
    @GetMapping("/result")
    public ApiResponse<PhotoTransformResultResponse> getResult(
            @RequestParam @NotBlank(message = "任务ID不能为空") String taskId) {
        log.info("查询证件照转化结果，任务ID: {}", taskId);
        PhotoTransformResultResponse response = photoTransformService.queryTransformResult(taskId);
        return ApiResponse.success(response);
    }
}
