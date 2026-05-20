# 架构与开发

## 分层结构

```
src/main/java/com/phototransform/
├── common/              # 公共组件（统一响应、业务异常、事件、全局异常处理）
├── config/              # 配置类（应用配置、存储、任务、Seedream、Supabase 配置）
├── controller/          # 控制器层（REST API 接口）
├── domain/              # 领域模型
│   └── entity/          # 实体类（JPA）
├── dto/                 # 数据传输对象（图像生成相关、照片转换相关）
├── enums/               # 枚举类（生成能力、模式、模型类型、背景色、任务状态）
├── repository/          # 数据访问层（JPA Repository + 派生查询）
└── service/             # 业务逻辑层（接口和实现）
    └── impl/            # 服务实现类
```

## 核心组件

**1. PhotoTransformController** — 照片转换 API 控制器
   - `POST /api/photo/transform` — 提交证件照转化任务（multipart/form-data）
   - `GET /api/photo/result` — 查询转化结果（?taskId=xxx）

**2. PhotoTransformService / Impl** — 核心业务逻辑
   - 参数校验、任务创建（taskId 生成、原图上传存储、实体持久化到 Supabase PostgreSQL）
   - 发布 `TaskCreatedEvent` 解耦异步处理
   - 将存储图片转为 base64 data URL 后调用 Seedream
   - prompt 构建、结果图片保存、任务状态更新
   - 结果查询（过期检查，只读不写 DB）
   - `@Scheduled` 定时清理超时/过期任务（先删存储文件，再删 DB）
   - `@EventListener(ApplicationReadyEvent)` 启动时清理残留任务

**3. SeedreamImageService / Impl** — 火山引擎 Seedream API 集成
   - 基于 `doubao-seedream-4.5` 模型，使用 `volcengine-java-sdk-ark-runtime`
   - 自动识别生成能力（文生图/图生图/多图生图 × 单图/组图）
   - 参数校验（prompt ≤ 2000 字符、参考图 ≤ 14 张、尺寸格式等）
   - 响应解析：顶层 error 检查 → 逐图 url/b64Json 判空 → SUCCESS/PARTIAL_SUCCESS/FAILED
   - 默认尺寸 `2K`（图生图最低要求 3,686,400 像素）

**4. AsyncTaskExecutor** — 异步任务执行器
   - `@EventListener` 监听 `TaskCreatedEvent`
   - `@Async("taskExecutor")` 在线程池中执行 `processTransformTask()`
   - 与 ServiceImpl 无循环依赖

**5. StorageService / 双实现** — 文件存储（通过 `app.storage.type` 切换）
   - `LocalStorageServiceImpl` — 本地文件系统存储（dev 用，`@ConditionalOnProperty(type=local)`）
   - `SupabaseStorageServiceImpl` — Supabase Storage REST API（prod 用，`@ConditionalOnProperty(type=supabase)`）
   - 接口：`store()`（MultipartFile / byte[]）、`readByUrl()`（HTTP GET / 本地读）、`deleteByUrl()`（清理用）

**6. GlobalExceptionHandler** — 全局异常处理
   - `BusinessException` → 200 + 业务错误码
   - `MethodArgumentNotValidException` / `ConstraintViolationException` → 400
   - `MissingServletRequestParameterException` → 400
   - `MaxUploadSizeExceededException` → 400
   - 未知异常 → 500

**7. PhotoTransformTask** — 领域实体（JPA @Entity）
   - 状态：`PROCESSING → SUCCESS / FAILED`
   - 字段：taskId（@Id, 主键）, originalImageUrl, resultImageUrl, status, modelType, backgroundColor, photoType, errorMessage, createdTime, updatedTime
   - 映射表：`photo_transform_task`（Hibernate ddl-auto=update 自动建表）

## 数据存储

- **生产环境**：Supabase PostgreSQL（Shared Pooler + HikariCP max=5）
- **开发/测试**：H2 内存数据库（`@ActiveProfiles("test")` 自动切换）
- ORM：Spring Data JPA（`ddl-auto=update`）
- 图片存储：Supabase Storage（bucket: photoX，公开访问）

## 关键文件位置

| 文件 | 位置 | 用途 |
|------|------|------|
| 启动类 | PhotoTransformApplication.java | Spring Boot 应用入口 |
| 控制器 | controller/PhotoTransformController.java | API 接口定义 |
| 核心服务 | service/impl/PhotoTransformServiceImpl.java | 任务创建、处理、查询、清理 |
| Seedream 集成 | service/impl/SeedreamImageServiceImpl.java | AI 图像生成 API 调用 |
| 异步执行器 | service/impl/AsyncTaskExecutor.java | 事件驱动异步处理 |
| Supabase 存储 | service/impl/SupabaseStorageServiceImpl.java | Supabase Storage REST API |
| 本地存储 | service/impl/LocalStorageServiceImpl.java | 本地文件读写（dev） |
| 任务实体 | domain/entity/PhotoTransformTask.java | JPA 实体，映射 photo_transform_task 表 |
| 仓库接口 | repository/PhotoTransformTaskRepository.java | JpaRepository + 派生查询 |
| 统一响应 | common/ApiResponse.java | API 响应格式 |
| 业务异常 | common/BusinessException.java | 自定义异常 |
| 全局异常处理 | common/GlobalExceptionHandler.java | 统一异常转换 |
| 任务事件 | common/TaskCreatedEvent.java | 异步解耦事件 |
| 应用配置 | config/AppConfig.java | 线程池、RestTemplate、CORS |
| Seedream 配置 | config/SeedreamClientConfig.java | API 密钥、模型、尺寸 |
| Supabase 配置 | config/SupabaseStorageProperties.java | Storage URL, service_role key, bucket |
| 存储配置 | config/AppStorageProperties.java | 存储类型、本地路径 |
| 任务配置 | config/AppTaskProperties.java | timeout/expiry 拆分、清理 cron |
| 生成请求 DTO | dto/ImageGenerationRequest.java | Seedream 请求参数封装 |
| 生成结果 DTO | dto/ImageGenerationResult.java | Seedream 响应结果封装 |

## 构建与开发

### 编译项目
```bash
mvn clean compile
```

### 运行应用
```bash
# 开发模式（端口 8081，dev profile：本地存储 + H2 或 Supabase DB）
mvn spring-boot:run

# 生产模式（端口 8080，prod profile：Supabase Storage + Supabase DB）
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# 打包后运行
mvn clean package
java -jar target/photo-transform-service-1.0.0-SNAPSHOT.jar
```

### 测试
```bash
# 运行所有测试
mvn test

# Mock 测试（快速，不含真实 API 调用，H2 内存库）
mvn -Dtest=PhotoTransformControllerTest test

# 真实 API 端到端测试（需 ARK_API_KEY + SUPABASE_DB_PASSWORD）
mvn -Dtest=PhotoTransformControllerRealApiTest test

# Supabase Storage 集成测试（需 SUPABASE_SERVICE_ROLE_KEY）
mvn -Dtest=SupabaseStorageIntegrationTest test
```

### 主要配置文件
- `application.yml` — 生产环境配置（端口 8080，Supabase DB + Storage）
- `application-dev.yml` — 开发环境配置（端口 8081，本地存储，DEV 日志）
- `application-test.yml` — 测试环境配置（H2 内存库，本地存储）

## 依赖管理

**主要依赖**：
- Spring Boot 2.7.18
- Spring Data JPA（JPA + HikariCP）
- PostgreSQL JDBC Driver（Supabase）
- H2 Database（测试用）
- Lombok（简化代码）
- Validation（参数校验）
- 火山方舟 SDK `volcengine-java-sdk-ark-runtime`（Seedream API）

**构建工具**：Maven 3.x
