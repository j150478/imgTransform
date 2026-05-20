# Photo Transform Service

证件照转化后端服务 — 基于 Spring Boot + 火山引擎 Seedream API 的 AI 证件照生成服务。

## 功能特性

- **AI 证件照生成** — 上传照片，调用 Seedream 4.5 模型生成标准证件照
- **异步任务管理** — 提交任务立即返回，后台处理，轮询查询结果
- **多背景色支持** — 蓝底（1）、红底（2）、白底（3）
- **多模型支持** — SEEDREAM_45（推荐）、NANO_PRO（待支持）

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心框架 |
| Java | 1.8 | 编程语言 |
| Maven | 3.x | 构建工具 |
| Lombok | latest | 简化代码 |
| Validation | latest | 参数校验 |
| volcengine-java-sdk-ark-runtime | LATEST | 火山方舟 Seedream API |

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- 火山引擎 API Key（环境变量 `ARK_API_KEY`）
- Supabase 数据库密码（环境变量 `SUPABASE_DB_PASSWORD`）

### 启动

```bash
# 设置 API Key
export ARK_API_KEY="your-api-key"
export SUPABASE_DB_PASSWORD="your-db-password"

# 开发模式（端口 8081）
mvn spring-boot:run

# 生产模式（端口 8080）
java -jar target/photo-transform-service-1.0.0-SNAPSHOT.jar
```

## API 文档

### 1. 提交证件照转化

```
POST /api/photo/transform
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | 是 | 照片文件（≤10MB，仅限图片） |
| backgroundColor | Integer | 否 | 背景色代码：1=蓝 2=红 3=白，默认 1 |
| modelType | String | 否 | 模型类型：SEEDREAM_45，默认 SEEDREAM_45 |
| photoType | String | 否 | 证件照类型 |

**curl 示例：**
```bash
curl -X POST http://localhost:8080/api/photo/transform \
  -F "file=@/path/to/photo.jpg" \
  -F "backgroundColor=1"
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "PT27EB5C90221E4D8A",
    "status": "PROCESSING"
  }
}
```

### 2. 查询转化结果

```
GET /api/photo/result?taskId={taskId}
```

**响应（处理中）：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "PROCESSING",
    "resultImageUrl": null,
    "errorMessage": null
  }
}
```

**响应（成功）：**
```json
{
  "code": 200,
  "data": {
    "status": "SUCCESS",
    "resultImageUrl": "http://localhost:8080/uploads/PTxxx_result.jpg",
    "errorMessage": null
  }
}
```

**响应（失败）：**
```json
{
  "code": 200,
  "data": {
    "status": "FAILED",
    "resultImageUrl": null,
    "errorMessage": "具体错误描述"
  }
}
```

**错误码：**

| code | 说明 |
|------|------|
| 200 | 成功（含业务数据或业务错误信息） |
| 400 | 参数校验失败 |
| 404 | 任务不存在 |
| 500 | 服务器内部错误 |

## 项目结构

```
src/main/java/com/phototransform/
├── PhotoTransformApplication.java              # 启动类
├── common/
│   ├── ApiResponse.java                        # 统一响应格式
│   ├── BusinessException.java                  # 业务异常
│   ├── GlobalExceptionHandler.java             # 全局异常处理
│   └── TaskCreatedEvent.java                   # 异步任务事件
├── config/
│   ├── AppConfig.java                          # 线程池、CORS、静态资源
│   ├── AppStorageProperties.java               # 存储路径配置
│   ├── AppTaskProperties.java                  # 任务过期/清理配置
│   └── SeedreamClientConfig.java               # Seedream API 配置
├── controller/
│   └── PhotoTransformController.java           # REST 控制器
├── domain/entity/
│   └── PhotoTransformTask.java                 # 任务实体
├── dto/
│   ├── ImageGenerationRequest.java             # Seedream 请求 DTO
│   ├── ImageGenerationResult.java              # Seedream 结果 DTO
│   ├── PhotoTransformRequest.java              # 转换请求 DTO
│   ├── PhotoTransformResponse.java             # 提交响应 DTO
│   └── PhotoTransformResultResponse.java       # 查询响应 DTO
├── enums/
│   ├── BackgroundColor.java                    # 背景色枚举（蓝/红/白）
│   ├── GenerationCapability.java               # 生成能力枚举
│   ├── GenerationMode.java                     # 生成模式枚举
│   ├── ModelType.java                          # 模型类型枚举
│   └── TransformStatus.java                    # 任务状态枚举
├── repository/
│   ├── PhotoTransformTaskRepository.java       # 仓库接口
│   └── impl/InMemoryTaskRepository.java        # 内存实现
└── service/
    ├── PhotoTransformService.java              # 转换服务接口
    ├── SeedreamImageService.java               # 图像生成接口
    ├── StorageService.java                     # 文件存储接口
    └── impl/
        ├── PhotoTransformServiceImpl.java      # 核心业务逻辑
        ├── SeedreamImageServiceImpl.java        # Seedream API 集成
        ├── AsyncTaskExecutor.java              # 异步任务执行器
        └── LocalStorageServiceImpl.java        # 本地文件存储
```

## 配置说明

### 环境变量

| 变量 | 说明 |
|------|------|
| `ARK_API_KEY` | 火山引擎 API Key（必填） |
| `SUPABASE_DB_PASSWORD` | Supabase 数据库密码（必填） |

### Seedream 配置（`seedream.*`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `apiKey` | `${ARK_API_KEY}` | API 密钥 |
| `baseUrl` | `https://ark.cn-beijing.volces.com/api/v3` | API 端点 |
| `modelName` | `doubao-seedream-4-5-251128` | 默认模型 |
| `defaultSize` | `2K` | 默认图片尺寸（图生图 ≥ 3,686,400 像素） |
| `defaultResponseFormat` | `url` | 响应格式（url / b64_json） |
| `defaultWatermark` | `false` | 默认水印开关 |
| `connectionTimeout` | `10000` | 连接超时（ms） |
| `readTimeout` | `60000` | 读取超时（ms） |

### 存储配置（`app.storage.*`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `type` | `local` | 存储类型 |
| `localPath` | `uploads/`（dev: `dev-uploads/`） | 本地存储路径 |
| `urlPrefix` | `http://localhost:8080/uploads/` | 访问 URL 前缀 |

### 任务配置（`app.task.*`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `expireHours` | 24（dev: 1） | 任务过期时间（小时） |
| `cleanupCron` | `0 0 2 * * ?` | 过期清理 cron |

## 开发指南

### 代码规范

- Controller 统一返回 `ApiResponse<T>`
- 业务异常抛出 `BusinessException(code, message)`
- `@Valid` / `@Validated` 参数校验
- 日志使用 slf4j（关键流程 info，异常 error）
- 所有 public 方法编写 Javadoc

### 测试

```bash
# 全量测试
mvn test

# 快速回归（Mock Seedream）
mvn -Dtest=PhotoTransformControllerTest test

# 真实 API 端到端测试（需 ARK_API_KEY）
mvn -Dtest=PhotoTransformControllerRealApiTest test
```

### 常见问题

**Q: 启动报 ARK_API_KEY 未配置？**
A: 设置环境变量 `export ARK_API_KEY="your-key"`。

**Q: 启动报数据库连接失败（password authentication failed）？**
A: 设置环境变量 `export SUPABASE_DB_PASSWORD="your-db-password"`。

**Q: 生成失败提示图片尺寸不足？**
A: 图生图最低要求 3,686,400 像素，检查 `seedream.defaultSize` 配置。

**Q: 如何查看 Seedream API 调用详情？**
A: 日志级别设为 DEBUG：`logging.level.com.phototransform: DEBUG`。

## 更新日志

### [1.0.0] - 2026-05

- 证件照提交/查询 API
- Seedream 4.5 图生图集成
- 异步任务处理（事件驱动 + 线程池）
- base64 data URL 图片传输
- 全局异常处理
- 内存任务仓库（后续替换为持久化实现）
- 控制器集成测试（Mock + 真实 API）
