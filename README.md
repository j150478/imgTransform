# Photo Transform Service

证件照转化后端服务 - 基于 Spring Boot 的高性能照片处理 API 服务

## 功能特性

- **照片转换任务管理** - 支持异步任务创建、状态查询和结果获取
- **多模型支持** - 集成 Seedream API，支持多种 AI 模型
- **RESTful API** - 符合 REST 设计规范的 API 接口
- **分层架构** - 清晰的 Controller-Service-Repository 分层设计

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心框架 |
| Java | 1.8 | 编程语言 |
| Maven | 3.x | 构建工具 |
| Lombok | 最新 | 简化代码 |
| Validation | 最新 | 参数校验 |

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- IDE（推荐 IntelliJ IDEA）

### 安装步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd photo-transform-service
```

2. **配置环境**
   - 复制 `application-dev.yml` 并根据需要修改配置
   - 配置 Seedream API 密钥等敏感信息

3. **编译运行**
```bash
# 编译
mvn clean compile

# 运行
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/photo-transform-service-1.0.0-SNAPSHOT.jar
```

4. **验证服务**
```bash
curl http://localhost:8080/actuator/health
```

## API 文档

### 主要端点

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/photo-transform` | 创建照片转换任务 |
| GET | `/api/v1/photo-transform/{taskId}` | 查询任务状态和结果 |
| GET | `/api/v1/photo-transform/{taskId}/result` | 获取转换结果 |

### 请求示例

**创建转换任务：**
```bash
curl -X POST http://localhost:8080/api/v1/photo-transform \
  -H "Content-Type: application/json" \
  -d '{
    "imageUrl": "https://example.com/photo.jpg",
    "modelType": "PORTRAIT",
    "parameters": {
      "style": "id_photo",
      "background": "blue"
    }
  }'
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PENDING",
    "createdAt": "2025-02-11T10:30:00"
  }
}
```

## 项目结构

```
photo-transform-service/
├── src/main/java/com/phototransform/
│   ├── PhotoTransformApplication.java    # 启动类
│   ├── common/                           # 公共类
│   │   ├── ApiResponse.java              # 统一响应
│   │   └── BusinessException.java        # 业务异常
│   ├── config/                           # 配置类
│   ├── controller/                       # 控制器层
│   ├── domain/                           # 领域模型
│   │   ├── dto/                          # 数据传输对象
│   │   ├── entity/                       # 实体类
│   │   └── enums/                        # 枚举类
│   ├── repository/                       # 数据访问层
│   └── service/                          # 业务逻辑层
│       └── impl/                         # 实现类
├── src/main/resources/
│   ├── application.yml                   # 主配置
│   └── application-dev.yml               # 开发环境配置
├── pom.xml                               # Maven 配置
└── README.md                             # 项目说明
```

## 配置说明

### 核心配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | 8080 |
| `seedream.api.key` | Seedream API 密钥 | - |
| `seedream.api.endpoint` | Seedream API 端点 | - |
| `logging.level.root` | 日志级别 | INFO |

### 配置文件示例

**application-dev.yml:**
```yaml
server:
  port: 8080

seedream:
  api:
    key: ${SEEDREAM_API_KEY:your-api-key-here}
    endpoint: ${SEEDREAM_API_ENDPOINT:https://api.seedream.com/v1}
    timeout: 30000

logging:
  level:
    com.phototransform: DEBUG
    org.springframework.web: DEBUG
```

## 开发指南

### 代码规范

- 遵循 [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- 使用 Lombok 简化代码
- 统一使用 ApiResponse 包装响应
- 异常统一使用 BusinessException

### 分支策略

- `main` - 生产分支
- `develop` - 开发分支
- `feature/*` - 功能分支
- `hotfix/*` - 热修复分支

### 提交流程

1. 从 `develop` 分支创建功能分支
2. 开发完成后，提交到远程分支
3. 创建 Pull Request 合并到 `develop`
4. 代码审查通过后合并

## 贡献指南

欢迎提交 Issue 和 Pull Request！

1. **Fork 本仓库**
2. **创建分支**: `git checkout -b feature/AmazingFeature`
3. **提交更改**: `git commit -m 'Add some AmazingFeature'`
4. **推送分支**: `git push origin feature/AmazingFeature`
5. **创建 Pull Request**

### 贡献规范

- 代码需通过所有测试
- 添加必要的注释和文档
- 遵循项目代码风格
- 提交信息清晰描述改动

## 常见问题

### Q: 如何配置 Seedream API 密钥？
**A:** 在 `application-dev.yml` 中配置 `seedream.api.key`，或使用环境变量 `SEEDREAM_API_KEY`。

### Q: 服务启动失败，端口被占用怎么办？
**A:** 修改 `application.yml` 中的 `server.port` 配置，使用其他端口。

### Q: 如何查看更详细的日志？
**A:** 在配置中调整日志级别：`logging.level.com.phototransform: DEBUG`

### Q: API 返回 500 错误怎么处理？
**A:** 检查：
1. Seedream API 密钥是否正确配置
2. 网络连接是否正常
3. 查看应用日志获取详细错误信息

## 更新日志

### [1.0.0] - 2025-02-11

#### 新增
- 照片转换任务管理功能
- 异步任务处理支持
- Seedream API 集成
- RESTful API 接口
- 完整的错误处理机制

## 许可证

本项目采用 [MIT](LICENSE) 许可证。

---

## 联系方式

如有问题或建议，欢迎提交 [Issue](https://gitee.com/your-username/photo-transform-service/issues) 或联系维护者。

**维护者**: [Your Name](mailto:your.email@example.com)

---

<p align="center">如果这个项目对你有帮助，请给个 ⭐ Star 支持一下！</p>
