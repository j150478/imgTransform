# 架构与开发

## 分层结构

```
src/main/java/com/phototransform/
├── common/              # 公共组件（统一响应、业务异常、事件、全局异常处理）
├── config/              # 配置类（应用配置、存储、任务、Seedream 客户端配置）
├── controller/          # 控制器层（REST API 接口）
├── domain/              # 领域模型
│   └── entity/          # 实体类
├── dto/                 # 数据传输对象（图像生成相关、照片转换相关）
├── enums/               # 枚举类（生成能力、模式、模型类型、背景色、任务状态）
├── repository/          # 数据访问层（接口 + 实现）
│   └── impl/            # 仓库实现
└── service/             # 业务逻辑层（接口和实现）
    └── impl/            # 服务实现类
```

## 核心组件

**1. PhotoTransformController** — 照片转换 API 控制器
   - `POST /api/photo/transform` — 提交证件照转化任务（multipart/form-data）
   - `GET /api/photo/result` — 查询转化结果（?taskId=xxx）

**2. PhotoTransformService / Impl** — 核心业务逻辑
   - 参数校验、任务创建（taskId 生成、原图保存、实体持久化）
   - 发布 `TaskCreatedEvent` 解耦异步处理
   - 将本地图片 URL 转为 base64 data URL 后调用 Seedream（外部无法访问 localhost）
   - prompt 构建、结果图片保存、任务状态更新
   - 结果查询（含过期检查）
   - 定时清理过期任务

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

**5. StorageService / LocalStorageServiceImpl** — 本地文件存储
   - 上传文件持久化、字节数据写入
   - `readByUrl(String url)` — 根据存储 URL 反向读取文件字节（用于 base64 转换）

**6. GlobalExceptionHandler** — 全局异常处理
   - `BusinessException` → 200 + 业务错误码
   - `MethodArgumentNotValidException` / `ConstraintViolationException` → 400
   - `MissingServletRequestParameterException` → 400
   - `MaxUploadSizeExceededException` → 400
   - 未知异常 → 500

**7. PhotoTransformTask** — 领域实体
   - 状态：`PROCESSING → SUCCESS / FAILED`
   - 字段：taskId, originalImageUrl, resultImageUrl, status, modelType, backgroundColor, photoType, errorMessage, createdTime, updatedTime

## 关键文件位置

| 文件 | 位置 | 用途 |
|------|------|------|
| 启动类 | PhotoTransformApplication.java | Spring Boot 应用入口 |
| 控制器 | controller/PhotoTransformController.java | API 接口定义 |
| 核心服务 | service/impl/PhotoTransformServiceImpl.java | 任务创建、处理、查询 |
| Seedream 集成 | service/impl/SeedreamImageServiceImpl.java | AI 图像生成 API 调用 |
| 异步执行器 | service/impl/AsyncTaskExecutor.java | 事件驱动异步处理 |
| 文件存储 | service/impl/LocalStorageServiceImpl.java | 本地文件读写 |
| 任务实体 | domain/entity/PhotoTransformTask.java | 任务数据模型 |
| 统一响应 | common/ApiResponse.java | API 响应格式 |
| 业务异常 | common/BusinessException.java | 自定义异常 |
| 全局异常处理 | common/GlobalExceptionHandler.java | 统一异常转换 |
| 任务事件 | common/TaskCreatedEvent.java | 异步解耦事件 |
| 应用配置 | config/AppConfig.java | 线程池、静态资源、CORS |
| Seedream 配置 | config/SeedreamClientConfig.java | API 密钥、模型、尺寸 |
| 存储配置 | config/AppStorageProperties.java | 存储路径、URL 前缀 |
| 任务配置 | config/AppTaskProperties.java | 过期时间、清理 cron |
| 内存仓库 | repository/impl/InMemoryTaskRepository.java | ConcurrentHashMap 实现 |
| 生成请求 DTO | dto/ImageGenerationRequest.java | Seedream 请求参数封装 |
| 生成结果 DTO | dto/ImageGenerationResult.java | Seedream 响应结果封装 |

## 构建与开发

### 编译项目
```bash
mvn clean compile
```

### 运行应用
```bash
# 开发模式（端口 8081）
mvn spring-boot:run

# 打包后运行
mvn clean package
java -jar target/photo-transform-service-1.0.0-SNAPSHOT.jar
```

### 测试
```bash
# 运行所有测试
mvn test

# Mock 测试（快速，不含真实 API 调用）
mvn -Dtest=PhotoTransformControllerTest test

# 真实 API 测试（需 ARK_API_KEY 环境变量）
mvn -Dtest=PhotoTransformControllerRealApiTest test
```

### 主要配置文件
- `application.yml` — 生产环境配置（端口 8080）
- `application-dev.yml` — 开发环境配置（端口 8081，DEBUG 日志，dev-uploads/ 存储）

## 依赖管理

**主要依赖**：
- Spring Boot 2.7.18
- Lombok（简化代码）
- Validation（参数校验）
- 火山方舟 SDK `volcengine-java-sdk-ark-runtime`（Seedream API）

**构建工具**：Maven 3.x
