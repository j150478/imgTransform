package com.phototransform.service;

import com.phototransform.domain.entity.PhotoTransformTask;
import com.phototransform.dto.PhotoTransformRequest;
import com.phototransform.dto.PhotoTransformResponse;
import com.phototransform.dto.PhotoTransformResultResponse;

/**
 * 照片转化服务接口
 * 
 * 定义证件照转化业务的核心操作契约
 * 负责任务的创建、处理和查询
 */
public interface PhotoTransformService {

    /**
     * 创建证件照转化任务
     * 
     * 接收用户上传的照片和转化参数，创建新的转化任务
     * 任务创建后立即返回，实际处理异步执行
     * 
     * @param request 转化请求参数，包含照片文件和转化配置
     * @return 任务创建响应，包含任务ID和初始状态
     */
    PhotoTransformResponse createTransformTask(PhotoTransformRequest request);

    /**
     * 执行证件照转化处理
     * 
     * 调用图像处理模型对照片进行证件照转化
     * 该方法应为异步调用，在独立线程或队列中执行
     * 
     * @param task 待处理的任务实体，包含原始图片信息和转化配置
     */
    void processTransformTask(PhotoTransformTask task);

    /**
     * 查询证件照转化结果
     * 
     * 根据任务ID查询任务的当前状态和处理结果
     * 
     * @param taskId 任务唯一标识
     * @return 任务状态和结果信息
     */
    PhotoTransformResultResponse queryTransformResult(String taskId);
}
