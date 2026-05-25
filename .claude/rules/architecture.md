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
        └── mock/        # Mock 占位实现（SmsService、PaymentService）
```

## 核心组件

**1. PhotoTransformController** — 照片转换 API 控制器
   - `POST /api/photo/transform` — 提交证件照转化任务（multipart/form-data）
   - `GET /api/photo/result` — 查询转化结果（?taskId=xxx）

**2. PhotoTransformService / Impl** — 核心业务逻辑
   - `createTransformTask()`: 参数校验、TaskIdGenerator 生成 ID、原图上传存储、实体持久化到 Supabase PostgreSQL
   - 发布 `TaskCreatedEvent`（仅传 taskId，不传实体）解耦异步处理
   - `processTransformTask(String taskId)`: 内部从 DB 加载实体 → 组装 base64 data URL → 构建 prompt → 调用 Seedream → 保存结果 → 更新状态
   - 依赖 `ImageFetcher` 分离网络 IO（结果图片下载），`IdPhotoPromptBuilder` 从 application.yml 加载 prompt 模板并做 photoType 路由
   - 结果查询（过期检查，只读不写 DB）
   - `@Scheduled` 定时清理超时/过期任务（先删存储文件，再删 DB），超时计算使用分钟精度（防止 double 截断）
   - `@EventListener(ApplicationReadyEvent)` 启动时清理残留任务

**3. SeedreamImageService / Impl** — 火山引擎 Seedream API 集成
   - 负责请求校验、能力自动识别、能力匹配验证等业务逻辑
   - SDK 调用委托给 `SeedreamClient` 适配层，不直接接触 ArkService

**4. SeedreamClient / Impl** — Seedream SDK 适配层
   - 封装 ArkService 生命周期（`@PostConstruct` 初始化）
   - SDK 请求构建（DTO → GenerateImagesRequest）和响应解析（ImagesResponse → DTO）
   - 错误内部消化，通过 `ImageGenerationResult.status==FAILED` 通知调用方
   - 不暴露任何 SDK 类型到接口外，所有交互使用自有 DTO

**5. IdPhotoPromptBuilder** — Prompt 模板构建器
   - `@Component` + `@ConfigurationProperties(prefix = "prompt")`，从 application.yml 加载 prompt 模板
   - 模板按 photoType 路由（key 不存在时 fallback 到 id-photo）
   - 模板分 system（正面指令）和 negative（负面约束）两段，支持 `{name}` / `{rgb}` 占位符

**6. QuotaService / QuotaManagerImpl** — 用户额度管理
   - 统一管理额度扣减（checkAndDeduct）、增加（addCredits）和初始化创建（create）
   - 独占 `UserQuotaRepository`，外部模块不直接操作额度表
   - 扣减和增加均使用悲观锁（`PESSIMISTIC_WRITE`），解决并发充值竞态问题
   - 增加和创建方法不带 `@Transactional`，由调用方控制事务边界

**7. AsyncTaskExecutor** — 异步任务执行器
   - `@EventListener` 监听 `TaskCreatedEvent`
   - 事件仅传 taskId，不传实体（避免 JPA 实体泄漏到事件层）
   - 不注入 Repository，委托 Service 内部加载实体并异步处理
   - `@Async("taskExecutor")` 在线程池中执行，与 ServiceImpl 无循环依赖

**8. StorageService / 双实现** — 文件存储（通过 `app.storage.type` 切换）
   - `LocalStorageServiceImpl` — 本地文件系统存储（dev 用，`@ConditionalOnProperty(type=local)`）
   - `SupabaseStorageServiceImpl` — Supabase Storage REST API（prod 用，`@ConditionalOnProperty(type=supabase)`）
   - 接口：`store()`（MultipartFile / byte[]）、`readByUrl()`（HTTP GET / 本地读）、`deleteByUrl()`（清理用）

**9. GlobalExceptionHandler** — 全局异常处理
   - `BusinessException` → 200 + 业务错误码
   - `MethodArgumentNotValidException` / `ConstraintViolationException` → 400
   - `MissingServletRequestParameterException` → 400
   - `MaxUploadSizeExceededException` → 400
   - 未知异常 → 500

**10. PhotoTransformTask** — 领域实体（JPA @Entity）
   - 状态：`PROCESSING → SUCCESS / FAILED`
   - 字段：taskId（@Id, 主键）, originalImageUrl, resultImageUrl, status, modelType, backgroundColor, photoType, errorMessage, createdTime, updatedTime
   - 映射表：`photo_transform_task`（Hibernate ddl-auto=update 自动建表）

**11. ImageFetcher / Impl** — 图片下载组件
   - 接口：`byte[] fetch(String url)`，从远程 URL 下载图片字节数据
   - 失败抛 `BusinessException(500)`，将网络 IO 从服务编排层分离到基础设施层

**12. TaskIdGenerator** — 统一 ID 生成器
   - `@Component`，单方法 `generate(String prefix)`
   - 格式：prefix + UUID 去连字符取前 16 位大写
   - PhotoTransformServiceImpl（prefix=PT）和 SeedreamImageServiceImpl（prefix=SD）共享

**13. StorageUtils** — 存储工具类
   - 提供 `static extractExtension(String)` 方法
   - LocalStorageServiceImpl 和 SupabaseStorageServiceImpl 共享，消除重复代码

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
| Seedream 业务层 | service/impl/SeedreamImageServiceImpl.java | 请求校验、能力识别、编排 |
| Seedream SDK 适配 | service/impl/SeedreamClientImpl.java | ArkService 生命周期、SDK 调用、响应转换 |
| 额度管理 | service/impl/QuotaManagerImpl.java | 统一额度扣减/增加/创建，悲观锁 |
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
| Prompt 构建器 | service/impl/IdPhotoPromptBuilder.java | 模板加载 + photoType 路由 + 占位符替换 |
| 图片下载组件 | service/ImageFetcher.java | 远程 URL 下载接口 |
| 图片下载实现 | service/impl/ImageFetcherImpl.java | HTTP GET 下载实现 |
| ID 生成器 | service/impl/TaskIdGenerator.java | 统一 ID 生成策略 |
| SDK 适配接口 | service/SeedreamClient.java | Seedream SDK 适配层接口 |
| 额度管理接口 | service/QuotaService.java | 额度扣减/增加/创建接口 |
| Mock 短信 | service/impl/mock/MockSmsService.java | SmsService 占位实现 |
| Mock 支付 | service/impl/mock/MockPaymentServiceImpl.java | PaymentService 占位实现 |
| 存储工具类 | service/impl/StorageUtils.java | extractExtension 共享方法 |
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
