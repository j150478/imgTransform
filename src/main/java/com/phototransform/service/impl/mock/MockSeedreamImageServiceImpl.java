package com.phototransform.service.impl.mock;

import com.phototransform.common.BusinessException;
import com.phototransform.common.TaskIdGenerator;
import com.phototransform.dto.ImageGenerationRequest;
import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.enums.GenerationCapability;
import com.phototransform.enums.GenerationStatus;
import com.phototransform.service.SeedreamImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Mock Seedream 图像生成服务实现。
 *
 * <p>生成占位图片替代真实 API 调用，用于本地开发和测试，不消耗 API 额度。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seedream.mode", havingValue = "mock")
public class MockSeedreamImageServiceImpl implements SeedreamImageService {

    private static final int PLACEHOLDER_SIZE = 512;
    private static final Color[] COLORS = {
        new Color(255, 182, 193), new Color(135, 206, 250),
        new Color(152, 251, 152), new Color(255, 218, 185),
        new Color(221, 160, 221), new Color(255, 255, 153),
    };

    private final TaskIdGenerator taskIdGenerator;

    @Override
    public ImageGenerationResult generate(ImageGenerationRequest request) {
        String taskId = taskIdGenerator.generate("SD");
        log.info("[MOCK_SEEDREAM] [{}] 模拟图像生成, prompt: {}", taskId, truncate(request.getPrompt()));

        try {
            // 1. 生成占位图片并编码为 base64
            byte[] imageBytes = generatePlaceholderImage(request.getPrompt());
            String b64Json = java.util.Base64.getEncoder().encodeToString(imageBytes);

            // 2. 构建成功结果
            ImageGenerationResult.GeneratedImage generatedImage = ImageGenerationResult.GeneratedImage.builder()
                    .index(0)
                    .b64Json(b64Json)
                    .width(PLACEHOLDER_SIZE)
                    .height(PLACEHOLDER_SIZE)
                    .contentType("image/jpeg")
                    .build();

            log.info("[MOCK_SEEDREAM] [{}] 模拟生成完成, bytes: {}", taskId, imageBytes.length);
            return ImageGenerationResult.builder()
                    .taskId(taskId)
                    .status(GenerationStatus.SUCCESS)
                    .images(Collections.singletonList(generatedImage))
                    .usedCapability(GenerationCapability.TEXT_TO_IMAGE)
                    .createdAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .model("mock")
                    .prompt(request.getPrompt())
                    .build();
        } catch (Exception e) {
            log.error("[MOCK_SEEDREAM] [{}] 模拟生成失败", taskId, e);
            return ImageGenerationResult.builder()
                    .taskId(taskId)
                    .status(GenerationStatus.FAILED)
                    .errorMessage("Mock 生成失败: " + e.getMessage())
                    .errorCode("500")
                    .build();
        }
    }

    @Override
    public ImageGenerationResult generateWithCapability(ImageGenerationRequest request, GenerationCapability capability) {
        return generate(request);
    }

    /**
     * 生成占位图片（带文字标签的彩色方块）
     */
    private byte[] generatePlaceholderImage(String prompt) throws Exception {
        BufferedImage image = new BufferedImage(PLACEHOLDER_SIZE, PLACEHOLDER_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 1. 根据 prompt hash 选背景色
        int idx = Math.abs(prompt.hashCode()) % COLORS.length;
        g.setColor(COLORS[idx]);
        g.fillRect(0, 0, PLACEHOLDER_SIZE, PLACEHOLDER_SIZE);

        // 2. 绘制文字标签
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String label = "[Mock Seedream]";
        FontMetrics fm = g.getFontMetrics();
        int x = (PLACEHOLDER_SIZE - fm.stringWidth(label)) / 2;
        g.drawString(label, x, PLACEHOLDER_SIZE / 2);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String promptPreview = truncate(prompt);
        fm = g.getFontMetrics();
        x = (PLACEHOLDER_SIZE - fm.stringWidth(promptPreview)) / 2;
        g.drawString(promptPreview, x, PLACEHOLDER_SIZE / 2 + 30);

        g.dispose();

        // 3. 输出为 JPEG 字节
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "JPEG", baos);
        return baos.toByteArray();
    }

    private static String truncate(String s) {
        return s != null && s.length() > 60 ? s.substring(0, 57) + "..." : s;
    }
}
