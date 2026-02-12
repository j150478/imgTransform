# SeedreamImageService API 文档

> Seedream 4.5 图像生成服务 API 参考

---

## 目录

- [服务概述](#服务概述)
- [核心能力](#核心能力)
- [API 参考](#api-参考)
  - [generate](#generate)
  - [generateWithCapability](#generatewithcapability)
  - [generateSingle](#generatesingle)
  - [generateSequential](#generatesequential)
- [数据模型](#数据模型)
  - [ImageGenerationRequest](#imagegenerationrequest)
  - [ImageGenerationResult](#imagegenerationresult)
  - [GenerationCapability](#generationcapability)
  - [GenerationMode](#generationmode)
- [错误处理](#错误处理)
- [示例代码](#示例代码)
- [参考链接](#参考链接)

---

## 服务概述

`SeedreamImageService` 是火山引擎 Seedream 4.5 模型的图像生成服务封装，提供统一的 API 接口，支持文生图、图生图、多图生图、组图生成等多种能力。

### 技术规格

| 项目 | 说明 |
|------|------|
| **服务端点** | `https://ark.cn-beijing.volces.com/api/v3` |
| **默认模型** | `doubao-seedream-4-5-251128` |
| **单图最大参考图** | 14 张 |
| **组图最大输出** | 15 张 |
| **支持分辨率** | 1K / 2K / 4K 及自定义 |

---

## 核心能力

### 单图生成模式 (`GenerationMode.SINGLE`)

| 能力 | 输入 | 输出 | 说明 |
|------|------|------|------|
| **文生图** | 文本描述 | 1 张图 | 纯文本生成 |
| **单图生图** | 1 张参考图 + 文本 | 1 张图 | 基于参考图生成 |
| **多图生图** | 2-14 张参考图 + 文本 | 1 张图 | 融合多张图特征 |

### 组图生成模式 (`GenerationMode.SEQUENTIAL`)

| 能力 | 输入 | 输出 | 说明 |
|------|------|------|------|
| **文生组图** | 文本描述 | 最多 15 张 | 纯文本生成组图 |
| **单图生组图** | 1 张参考图 + 文本 | 最多 14 张 | 基于单图生成组图 |
| **多图生组图** | 2-14 张参考图 + 文本 | 组 图总数 ≤ 15 | 融合多图生成组图 |

### 约束条件

```
参考图数量限制:
  - 文生图: 0 张
  - 单图生图: 1 张
  - 多图生图/组图: 2-14 张

组图总数限制:
  - 输入参考图数量 + 生成图片数量 ≤ 15

分辨率限制:
  - 总像素范围: [921600, 16777216] (即 [1280x720, 4096x4096])
  - 宽高比范围: [1/16, 16]
```

---

## API 参考

### generate

统一图像生成入口方法，自动识别生成能力。

```java
ImageGenerationResult generate(ImageGenerationRequest request)
```

#### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `request` | ImageGenerationRequest | 是 | 图像生成请求参数 |

#### 自动识别逻辑

1. **判断参考图数量**
   - 0 张 → 文生图
   - 1 张 → 单图生图
   - 2-14 张 → 多图生图

2. **判断生成模式**
   - `mode == SINGLE` → 单图生成
   - `mode == SEQUENTIAL` → 组图生成
   - 未指定时根据 `n` 判断（n > 1 则组图）

#### 返回值

| 类型 | 说明 |
|------|------|
| ImageGenerationResult | 图像生成结果，包含生成的图片列表和元数据 |

#### 异常

| 异常类型 | 说明 |
|----------|------|
| BusinessException | 参数校验失败或生成失败时抛出 |

#### 示例

```java
// 文生图
ImageGenerationRequest request = ImageGenerationRequest.builder()
    .prompt("一只可爱的猫咪")
    .mode(GenerationMode.SINGLE)
    .build();
ImageGenerationResult result = seedreamImageService.generate(request);

// 图生图
ImageGenerationRequest request = ImageGenerationRequest.builder()
    .prompt("将这张图片转换成油画风格")
    .referenceImages(Collections.singletonList(imageUrl))
    .mode(GenerationMode.SINGLE)
    .build();

// 文生组图
ImageGenerationRequest request = ImageGenerationRequest.builder()
    .prompt("一只可爱的猫咪在不同场景下")
    .mode(GenerationMode.SEQUENTIAL)
    .n(4)
    .build();
```

---

### generateWithCapability

指定生成能力的图像生成方法。

```java
ImageGenerationResult generateWithCapability(
    ImageGenerationRequest request, 
    GenerationCapability capability
)
```

#### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `request` | ImageGenerationRequest | 是 | 图像生成请求参数 |
| `capability` | GenerationCapability | 是 | 指定的生成能力 |

#### 支持的生成能力

| 能力枚举 | 参考图数量 | 生成模式 | 说明 |
|----------|------------|----------|------|
| `TEXT_TO_IMAGE` | 0 | SINGLE | 文生图 |
| `TEXT_TO_IMAGE_SET` | 0 | SEQUENTIAL | 文生组图 |
| `SINGLE_IMAGE_TO_IMAGE` | 1 | SINGLE | 单图生图 |
| `SINGLE_IMAGE_TO_IMAGE_SET` | 1 | SEQUENTIAL | 单图生组图 |
| `MULTI_IMAGE_TO_IMAGE` | 2-14 | SINGLE | 多图生图 |
| `MULTI_IMAGE_TO_IMAGE_SET` | 2-14 | SEQUENTIAL | 多图生组图 |

#### 返回值

| 类型 | 说明 |
|------|------|
| ImageGenerationResult | 图像生成结果 |

#### 异常

| 异常类型 | 说明 |
|----------|------|
| IllegalArgumentException | 指定的能力与请求参数不匹配时抛出 |

#### 示例

```java
// 明确使用多图生图能力
ImageGenerationRequest request = ImageGenerationRequest.builder()
    .prompt("将这些图片的风格融合到一张图中")
    .referenceImages(Arrays.asList(imageUrl1, imageUrl2, imageUrl3))
    .build();

ImageGenerationResult result = seedreamImageService.generateWithCapability(
    request,
    GenerationCapability.MULTI_IMAGE_TO_IMAGE
);

// 明确使用单图生组图能力
ImageGenerationRequest request = ImageGenerationRequest.builder()
    .prompt("基于这张图片生成不同场景下的变体")
    .referenceImages(Collections.singletonList(imageUrl))
    .n(6)
    .build();

ImageGenerationResult result = seedreamImageService.generateWithCapability(
    request,
    GenerationCapability.SINGLE_IMAGE_TO_IMAGE_SET
);
```

---

### generateSingle

单图生成简化方法。

```java
default ImageGenerationResult generateSingle(String prompt)
```

#### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `prompt` | String | 是 | 提示词，描述要生成的图像内容 |

#### 返回值

| 类型 | 说明 |
|------|------|
| ImageGenerationResult | 单张图像的生成结果 |

#### 示例

```java
ImageGenerationResult result = seedreamImageService.generateSingle("一只可爱的猫咪");
```

---

### generateSequential

组图生成简化方法。

```java
default ImageGenerationResult generateSequential(String prompt, Integer n)
```

#### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `prompt` | String | 是 | 提示词，描述要生成的图像内容 |
| `n` | Integer | 是 | 生成图片数量（1-15） |

#### 返回值

| 类型 | 说明 |
|------|------|
| ImageGenerationResult | 多张图像的生成结果 |

#### 异常

| 异常类型 | 说明 |
|----------|------|
| IllegalArgumentException | 当 n 不在有效范围内时抛出 |

#### 示例

```java
ImageGenerationResult result = seedreamImageService.generateSequential(
    "一只可爱的猫咪在不同场景下", 
    4
);
```

---

## 数据模型

### ImageGenerationRequest

图像生成请求参数。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `prompt` | String | 是 | - | 提示词，描述要生成的图像内容 |
| `referenceImages` | List<String> | 否 | null | 参考图片 URL 列表（1-14张） |
| `mode` | GenerationMode | 否 | null | 生成模式（SINGLE/SEQUENTIAL） |
| `n` | Integer | 否 | 1 | 生成图片数量（1-15） |
| `quality` | String | 否 | "standard" | 图像质量（standard/hd） |
| `style` | String | 否 | "vivid" | 风格（vivid/natural） |
| `seed` | Integer | 否 | null | 随机种子，保证可重复性 |
| `size` | String | 否 | "1024x1024" | 图像尺寸 |
| `responseFormat` | String | 否 | "url" | 响应格式（url/b64_json） |
| `user` | String | 否 | null | 用户标识 |
| `watermark` | Boolean | 否 | false | 是否添加水印 |

---

### ImageGenerationResult

图像生成响应结果。

| 字段 | 类型 | 说明 |
|------|------|------|
| `taskId` | String | 任务唯一标识 |
| `status` | String | 生成状态（success/failed） |
| `images` | List<GeneratedImage> | 生成的图片列表 |
| `errorCode` | String | 错误码（失败时） |
| `errorMessage` | String | 错误信息（失败时） |
| `generatedAt` | LocalDateTime | 生成时间戳 |

#### GeneratedImage

| 字段 | 类型 | 说明 |
|------|------|------|
| `url` | String | 图片 URL |
| `b64Json` | String | Base64 编码的图片数据 |
| `width` | Integer | 图片宽度 |
| `height` | Integer | 图片高度 |
| `format` | String | 图片格式（png/jpeg） |
| `seed` | Integer | 随机种子值 |
| `revisedPrompt` | String | 模型优化后的提示词 |

---

### GenerationCapability

生成能力枚举，用于显式指定生成方式。

| 枚举值 | 参考图数量 | 生成模式 | 说明 |
|--------|------------|----------|------|
| `TEXT_TO_IMAGE` | 0 | SINGLE | 文生图 |
| `TEXT_TO_IMAGE_SET` | 0 | SEQUENTIAL | 文生组图 |
| `SINGLE_IMAGE_TO_IMAGE` | 1 | SINGLE | 单图生图 |
| `SINGLE_IMAGE_TO_IMAGE_SET` | 1 | SEQUENTIAL | 单图生组图 |
| `MULTI_IMAGE_TO_IMAGE` | 2-14 | SINGLE | 多图生图 |
| `MULTI_IMAGE_TO_IMAGE_SET` | 2-14 | SEQUENTIAL | 多图生组图 |

---

### GenerationMode

生成模式枚举。

| 枚举值 | 说明 |
|--------|------|
| `SINGLE` | 单图生成 |
| `SEQUENTIAL` | 组图生成（连续图像生成） |

---

## 错误处理

### 异常类型

| 异常类 | 说明 | 场景 |
|--------|------|------|
| `BusinessException` | 业务异常 | 参数校验失败、生成失败 |
| `IllegalArgumentException` | 非法参数 | 枚举值不匹配、超出范围 |

### 错误码

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| `PARAM_INVALID` | 参数校验失败 | 检查请求参数是否符合规范 |
| `GENERATION_FAILED` | 图像生成失败 | 检查提示词或参考图片是否合规 |
| `API_KEY_INVALID` | API Key 无效 | 检查配置的种子API Key |
| `RATE_LIMIT` | 请求频率限制 | 降低请求频率 |

---

## 示例代码

### 1. 文生图

```java
ImageGenerationRequest request = ImageGenerationRequest.builder()
    .prompt("一只可爱的橘猫在阳光下打盹")
    .mode(GenerationMode.SINGLE)
    .size("1024x1024")
    .build();

ImageGenerationResult result = seedreamImageService.generate(request);

if ("success".equals(result.getStatus())) {
    String imageUrl = result.getImages().get(0).getUrl();
    System.out.println("生成成功，图片URL: " + imageUrl);
}
```

### 2. 图生图

```java
ImageGenerationRequest request = ImageGenerationRequest.builder()
    .prompt("将这张图片转换成油画风格")
    .referenceImages(Collections.singletonList("https://example.com/input.jpg"))
    .mode(GenerationMode.SINGLE)
    .build();

ImageGenerationResult result = seedreamImageService.generate(request);
```

### 3. 多图生图

```java
List<String> referenceImages = Arrays.asList(
    "https://example.com/style1.jpg",
    "https://example.com/style2.jpg",
    "https://example.com/style3.jpg"
);

ImageGenerationRequest request = ImageGenerationRequest.builder()
    .prompt("融合这些图片的风格生成一张新图")
    .referenceImages(referenceImages)
    .mode(GenerationMode.SINGLE)
    .build();

// 显式指定多图生图能力
ImageGenerationResult result = seedreamImageService.generateWithCapability(
    request,
    GenerationCapability.MULTI_IMAGE_TO_IMAGE
);
```

### 4. 组图生成

```java
ImageGenerationRequest request = ImageGenerationRequest.builder()
    .prompt("一只可爱的猫咪在不同场景下：客厅、花园、卧室、厨房")
    .mode(GenerationMode.SEQUENTIAL)
    .n(4)  // 生成4张组图
    .size("1024x1024")
    .build();

ImageGenerationResult result = seedreamImageService.generate(request);

List<ImageGenerationResult.GeneratedImage> images = result.getImages();
for (int i = 0; i < images.size(); i++) {
    System.out.println("图片 " + (i + 1) + ": " + images.get(i).getUrl());
}
```

### 5. 高级参数设置

```java
ImageGenerationRequest request = ImageGenerationRequest.builder()
    .prompt("高清写实风格的未来城市夜景")
    .mode(GenerationMode.SINGLE)
    .size("2048x2048")  // 2K 分辨率
    .quality("hd")       // 高清质量
    .style("vivid")      // 生动风格
    .seed(42)            // 固定随机种子，保证可重复
    .watermark(false)    // 不添加水印
    .responseFormat("url")  // 返回 URL 格式
    .user("user_123")    // 用户标识
    .build();

ImageGenerationResult result = seedreamImageService.generate(request);
```

### 6. 错误处理

```java
try {
    ImageGenerationRequest request = ImageGenerationRequest.builder()
        .prompt("生成一张图片")
        .build();
    
    ImageGenerationResult result = seedreamImageService.generate(request);
    
    if ("success".equals(result.getStatus())) {
        // 处理成功结果
        System.out.println("生成成功: " + result.getImages().get(0).getUrl());
    } else {
        // 处理失败结果
        System.err.println("生成失败: " + result.getErrorMessage());
    }
} catch (BusinessException e) {
    // 业务异常处理
    System.err.println("业务错误: " + e.getMessage());
} catch (IllegalArgumentException e) {
    // 参数异常处理
    System.err.println("参数错误: " + e.getMessage());
} catch (Exception e) {
    // 其他异常处理
    System.err.println("系统错误: " + e.getMessage());
}
```

---

## 参考链接

### 官方文档

- [Seedream 4.5 API 参考](https://www.volcengine.com/docs/82379/1541523?lang=zh) - 火山引擎官方 API 文档
- [方舟大模型服务平台](https://www.volcengine.com/docs/82379) - 平台概览与快速入门

### 相关类

- `SeedreamImageService` - 服务接口
- `SeedreamImageServiceImpl` - 服务实现
- `SeedreamClientConfig` - 客户端配置
- `ImageGenerationRequest` - 请求 DTO
- `ImageGenerationResult` - 响应 DTO
- `GenerationCapability` - 生成能力枚举
- `GenerationMode` - 生成模式枚举

---

## 版本历史

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.0.0 | 2026-02-12 | 初始版本，支持 Seedream 4.5 全部能力 |

---

**文档生成时间**: 2026-02-12  
**服务版本**: 1.0.0-SNAPSHOT  
**作者**: PhotoTransform Team
