# Photo Transform Service

证件照转化后端服务 — 基于 Spring Boot + 火山引擎 Seedream API + Supabase 的 AI 证件照生成服务。

## 功能特性

- **AI 证件照生成** — 上传照片，调用 Seedream 4.5 模型生成标准证件照
- **异步任务管理** — 提交任务立即返回，后台处理，轮询查询结果
- **多背景色支持** — 蓝底（1）、红底（2）、白底（3）
- **多模型支持** — SEEDREAM_45（推荐）、NANO_PRO（待支持）
- **Supabase 持久化** — PostgreSQL 存储任务数据，Storage 存储图片
- **定时清理** — 超时 PROCESSING 任务标记失败，过期记录自动删除

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心框架 |
| Java | 1.8 | 编程语言 |
| Maven | 3.x | 构建工具 |
| Spring Data JPA | 2.7.18 | ORM 框架 |
| PostgreSQL / Supabase | — | 生产数据库 |
| H2 Database | — | 测试数据库（内存） |
| Supabase Storage | — | 图片存储（公开 bucket） |
| Lombok | latest | 简化代码 |
| Validation | latest | 参数校验 |
| volcengine-java-sdk-ark-runtime | 2.0.4 | 火山方舟 Seedream API |

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- 火山引擎 API Key（环境变量 `ARK_API_KEY`）
- Supabase 数据库密码（环境变量 `SUPABASE_DB_PASSWORD`）
- Supabase 存储密钥（环境变量 `SUPABASE_SERVICE_ROLE_KEY`，生产环境）

### 启动

```bash
# 设置环境变量
export ARK_API_KEY="your-api-key"
export SUPABASE_DB_PASSWORD="your-db-password"
export SUPABASE_SERVICE_ROLE_KEY="your-service-role-key"

# 开发模式（端口 8081，本地存储）
mvn spring-boot:run

# 生产模式（端口 8080，Supabase Storage）
mvn spring-boot:run -Dspring-boot.run.profiles=prod
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

**响应（成功 — Supabase 公开 URL）：**
```json
{
  "code": 200,
  "data": {
    "status": "SUCCESS",
    "resultImageUrl": "https://tbwbftcrpdwojrmscfuk.supabase.co/storage/v1/object/public/photoX/results/PTxxx_result.jpg",
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

**响应（已过期）：**
```json
{
  "code": 200,
  "data": {
    "status": "FAILED",
    "resultImageUrl": null,
    "errorMessage": "任务已过期"
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
│   ├── TaskCreatedEvent.java                   # 异步任务事件
│   ├── TaskIdGenerator.java                    # ID 生成器
│   └── StorageUtils.java                       # 存储工具类
├── config/
│   ├── AppConfig.java                          # 线程池、RestTemplate、CORS
│   ├── AppStorageProperties.java               # 存储类型/路径配置
│   ├── AppTaskProperties.java                  # timeout/expiry/清理 cron
│   ├── SeedreamClientConfig.java               # Seedream API 配置
│   └── SupabaseStorageProperties.java          # Supabase Storage URL/key/bucket
├── controller/
│   └── PhotoTransformController.java           # REST 控制器
├── domain/entity/
│   └── PhotoTransformTask.java                 # JPA 实体（→ photo_transform_task 表）
├── dto/
│   ├── ImageGenerationRequest.java             # Seedream 请求 DTO
│   ├── ImageGenerationResult.java              # Seedream 结果 DTO
│   ├── PhotoTransformRequest.java              # 转换请求 DTO
│   ├── PhotoTransformResponse.java             # 提交响应 DTO
│   └── PhotoTransformResultResponse.java       # 查询响应 DTO
├── enums/
│   ├── BackgroundColor.java                    # 背景色枚举
│   ├── GenerationCapability.java               # 生成能力枚举
│   ├── GenerationMode.java                     # 生成模式枚举
│   ├── ModelType.java                          # 模型类型枚举
│   └── TransformStatus.java                    # 任务状态枚举
├── repository/
│   └── PhotoTransformTaskRepository.java       # JPA Repository
└── service/
    ├── ImageFetcher.java                       # 图片下载接口
    ├── PhotoTransformService.java              # 转换服务接口
    ├── SeedreamImageService.java               # 图像生成接口
    ├── StorageService.java                     # 文件存储接口
    └── impl/
        ├── AsyncTaskExecutor.java              # 异步任务执行器
        ├── ImageFetcherImpl.java               # 图片下载实现
        ├── LocalStorageServiceImpl.java        # 本地文件存储（dev）
        ├── PhotoTransformServiceImpl.java      # 核心业务逻辑
        ├── SeedreamImageServiceImpl.java        # Seedream API 集成
        └── SupabaseStorageServiceImpl.java     # Supabase Storage 实现（prod）
```

## 配置说明

### 环境变量

| 变量 | 说明 |
|------|------|
| `ARK_API_KEY` | 火山引擎 API Key（必填） |
| `SUPABASE_DB_PASSWORD` | Supabase 数据库密码（必填） |
| `SUPABASE_SERVICE_ROLE_KEY` | Supabase Storage 服务端密钥（生产环境必填） |

### 数据源配置（`spring.datasource.*`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `url` | `jdbc:postgresql://...pooler.supabase.com:5432/postgres?user=...&sslmode=require` | Supabase Pooler JDBC URL |
| `driver-class-name` | `org.postgresql.Driver` | PostgreSQL 驱动 |
| `hikari.maximum-pool-size` | `5` | 连接池最大连接数 |
| `hikari.minimum-idle` | `2` | 连接池最小空闲数 |

### JPA 配置（`spring.jpa.*`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `hibernate.ddl-auto` | `update` | 自动更新表结构 |

### Seedream 配置（`seedream.*`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `apiKey` | `${ARK_API_KEY}` | API 密钥 |
| `baseUrl` | `https://ark.cn-beijing.volces.com/api/v3` | API 端点 |
| `modelName` | `doubao-seedream-4-5-251128` | 默认模型 |
| `defaultSize` | `2K` | 默认图片尺寸 |
| `defaultResponseFormat` | `url` | 响应格式（url / b64_json） |
| `connectionTimeout` | `10000` | 连接超时（ms），通过 ArkService builder 生效 |
| `readTimeout` | `60000` | 读取超时（ms），通过 ArkService builder 生效 |

### 存储配置（`app.storage.*`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `type` | `local` (dev) / `supabase` (prod) | 存储类型 |
| `localPath` | `uploads/`（dev: `dev-uploads/`） | 本地存储路径（type=local） |
| `urlPrefix` | `http://localhost:8080/uploads/` | 本地 URL 前缀（type=local） |

### Supabase Storage 配置（`supabase.storage.*`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `url` | `https://xxx.supabase.co` | Supabase 项目 URL |
| `serviceRoleKey` | `${SUPABASE_SERVICE_ROLE_KEY}` | 服务端 API Key |
| `bucket` | `photoX` | 存储桶名称 |

### 任务配置（`app.task.*`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `timeoutHours` | `1`（dev: `0.5`） | PROCESSING 超时时间（小时） |
| `expiryHours` | `24`（dev: `1`） | 已完成任务保留时间（小时） |
| `cleanupCron` | `0 0 * * * ?` | 过期清理 cron |

## 开发指南

### 代码规范

- Controller 统一返回 `ApiResponse<T>`
- 业务异常抛出 `BusinessException(code, message)`
- `@Valid` / `@Validated` 参数校验
- 日志使用 slf4j（关键流程 info，异常 error）
- 所有 public 方法编写 Javadoc
- 实现类方法步骤编号注释

### 测试

```bash
# 全量测试
mvn test

# 快速回归（Mock Seedream，H2 内存库）
mvn -Dtest=PhotoTransformControllerTest test

# 真实 API 端到端测试（需 ARK_API_KEY + SUPABASE_DB_PASSWORD）
mvn -Dtest=PhotoTransformControllerRealApiTest test

# Supabase Storage 集成测试（需 SUPABASE_SERVICE_ROLE_KEY）
mvn -Dtest=SupabaseStorageIntegrationTest test
```

### 常见问题

**Q: 启动报 ARK_API_KEY 未配置？**
A: 设置环境变量 `export ARK_API_KEY="your-key"`。

**Q: 启动报数据库连接失败（password authentication failed）？**
A: 设置环境变量 `export SUPABASE_DB_PASSWORD="your-db-password"`。

**Q: 上传图片报 Invalid Compact JWS / Bucket not found？**
A: 检查 `SUPABASE_SERVICE_ROLE_KEY` 是否正确（应为 JWT 格式，从 Supabase Dashboard → Project Settings → API → service_role 获取）。

**Q: 生成失败提示图片尺寸不足？**
A: 图生图最低要求 3,686,400 像素，检查 `seedream.defaultSize` 配置。

## 更新日志

### [1.0.0] - 2026-05

- 证件照提交/查询 API
- Seedream 4.5 图生图集成
- 异步任务处理（事件驱动 + 线程池）
- Supabase PostgreSQL 持久化存储（JPA + HikariCP）
- Supabase Storage 图片存储（REST API，公开 bucket）
- timeout/expiry 拆分 + cron 定时清理 + 启动残留清理
- 双存储实现：Local（dev）+ Supabase（prod），`@ConditionalOnProperty` 切换
- H2 内存数据库测试 profile
- 全局异常处理
- 控制器集成测试（Mock + 真实 API + Supabase Storage）
