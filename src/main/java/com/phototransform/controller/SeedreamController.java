package com.phototransform.controller;

import com.phototransform.common.ApiResponse;
import com.phototransform.dto.TextToImageRequest;
import com.phototransform.dto.TextToImageResponse;
import com.phototransform.service.ImageGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/**
 * Seedream 文生图 REST API 控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/seedream")
@RequiredArgsConstructor
public class SeedreamController {

    private final ImageGenerationService imageGenerationService;

    /**
     * 提交文生图任务
     *
     * @param request 文生图请求（prompt + 可选 size/n）
     * @param userId  用户 ID（由 AuthInterceptor 注入）
     * @return 任务 ID 和状态
     */
    @PostMapping("/generate")
    public ApiResponse<TextToImageResponse> generate(
            @Valid @RequestBody TextToImageRequest request,
            @RequestAttribute("userId") Long userId) {
        log.info("[CONTROLLER] 用户 {} 提交文生图请求, prompt: {}...",
                userId, request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())));
        TextToImageResponse response = imageGenerationService.createTask(request, userId);
        return ApiResponse.success(response);
    }

    /**
     * 查询文生图结果
     *
     * @param taskId 任务唯一标识
     * @return 任务状态和结果图片列表
     */
    @GetMapping("/result")
    public ApiResponse<TextToImageResponse> getResult(
            @RequestParam @NotBlank(message = "任务ID不能为空") String taskId) {
        log.info("查询文生图结果, taskId: {}", taskId);
        TextToImageResponse response = imageGenerationService.queryResult(taskId);
        return ApiResponse.success(response);
    }
}
