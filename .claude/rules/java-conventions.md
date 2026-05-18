---
paths:
  - "src/**/*.java"
---

# Java 代码规范与分层约束

## 代码规范

- 遵循阿里 Java 规范（命名、常量、日志、异常、POJO）
- 实体类必须使用 Lombok（@Data、@Builder）
- Controller 所有接口统一返回 ApiResponse<T>
- 业务异常统一抛 BusinessException，禁止 RuntimeException
- Controller 不处理异常，统一由 GlobalExceptionHandler 处理
- 所有 public 方法必须编写 Javadoc
- 实现类方法需按步骤编号说明逻辑（1、2、3...）
- 使用 slf4j，禁止 System.out.println
- 关键流程记录 info 日志，异常必须记录 error 日志
- 参数校验使用 javax.validation
- Controller 参数必须加 @Valid
- 禁止新增未讨论依赖

## 分层约束

```
controller → service → repository
```

- Controller 不写业务逻辑
- Service 负责业务编排
- Repository 仅负责数据访问
- DTO 仅用于接口层
- domain 仅用于领域模型
