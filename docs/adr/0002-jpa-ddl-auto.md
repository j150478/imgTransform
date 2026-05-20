# ADR 0002: JPA + ddl-auto=update 替代内存存储

## Status

Accepted

## Context

项目使用 ConcurrentHashMap 内存存储任务数据，进程重启即丢失。需要持久化到 PostgreSQL (Supabase)。

可选方案：JPA 或 MyBatis。Schema 迁移：Flyway 或 ddl-auto。

## Decision

- 使用 Spring Data JPA（非 MyBatis）：仅两个 CRUD 方法，无复杂查询
- 使用 `ddl-auto=update`（非 Flyway）：个人项目快速迭代，接受自动 schema 管理
- taskId 做主键（非自增 Long），业务层生成
- 枚举用 STRING 存储（非 ORDINAL），避免枚举重排导致数据损坏

## Consequences

- 实体加 JPA 注解（@Entity, @Id, @Enumerated 等）
- Repository 接口改为 extends JpaRepository
- 删除 InMemoryTaskRepository，无过渡期
- ddl-auto=update 在生产环境有风险（不可审计、不可回滚），当前阶段可接受
