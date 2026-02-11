package com.phototransform.controller;

import com.phototransform.common.ApiResponse;
import com.phototransform.domain.dto.PhotoTransformRequest;
import com.phototransform.domain.dto.PhotoTransformResponse;
import com.phototransform.domain.dto.PhotoTransformResultResponse;
import com.phototransform.service.PhotoTransformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

/**
 * 证件照转化 REST API 控制器
 * 
 * 提供证件照转化相关的 HTTP 接口，包括：
 * - 提交证件照转化任务
 * - 查询转化任务状态和结果
 * 
 * 所有接口返回统一的 ApiResponse 包装对象
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
     * 接收用户上传的照片和转化参数，创建新的证件照转化任务。
     * 任务创建后立即返回任务ID，实际处理异步执行。
     * 
     * HTTP 请求规范：
     * - Method: POST
     * - URL: /api/photo/transform
     * - Content-Type: multipart/form-data
     * - 参数:
     *   - file: 图片文件（必填）
     *   - modelType: 模型类型（必填）
     *   - backgroundColor: 背景颜色（可选）
     *   - photoType: 证件照类型（可选）
     * 
     * 处理流程：
     * 1. 接收并校验请求参数（文件格式、大小等）
     * 2. 调用 PhotoTransformService 创建转化任务
     * 3. 返回包含 taskId 的成功响应
     * 
     * @param request 转化请求参数，包含文件和配置信息
     * @return 统一响应对象，包含任务ID和状态
     */
    @PostMapping("/transform")
    public ApiResponse<PhotoTransformResponse> transform(@ModelAttribute PhotoTransformRequest request) {
        log.info("接收到证件照转化请求");
        
        // 参数校验逻辑（待实现）
        // - 校验文件是否为空
        // - 校验文件格式（JPEG、PNG）
        // - 校验文件大小
        
        // 调用服务层创建任务
        PhotoTransformResponse response = photoTransformService.createTransformTask(request);
        
        return ApiResponse.success(response);
    }

    /**
     * 查询证件照转化结果
     * 
     * 根据任务ID查询证件照转化任务的处理状态和结果。
     * 支持查询 processing、success、failed 三种状态。
     * 
     * HTTP 请求规范：
     * - Method: GET
     * - URL: /api/photo/result
     * - 参数:
     *   - taskId: 任务唯一标识（必填，路径参数或查询参数）
     * 
     * 响应状态说明：
     * - PROCESSING: 任务正在处理中，客户端应稍后重试查询
     * - SUCCESS: 处理成功，响应中包含结果图片URL
     * - FAILED: 处理失败，响应中包含错误信息
     * 
     * 处理流程：
     * 1. 接收并校验 taskId 参数
     * 2. 调用 PhotoTransformService 查询任务状态和结果
     * 3. 组装并返回查询结果
     * 
     * @param taskId 任务唯一标识
     * @return 统一响应对象，包含任务状态、结果URL或错误信息
     */
    @GetMapping("/result")
    public ApiResponse<PhotoTransformResultResponse> getResult(
            @RequestParam @NotBlank(message = "任务ID不能为空") String taskId) {
        log.info("查询证件照转化结果，任务ID: {}", taskId);
        
        // 参数校验逻辑（待实现）
        // - 校验 taskId 格式合法性
        
        // 调用服务层查询结果
        PhotoTransformResultResponse response = photoTransformService.queryTransformResult(taskId);
        
        return ApiResponse.success(response);
    }
}
