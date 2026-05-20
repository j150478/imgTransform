# ADR 0003: Supabase Storage 替代本地文件存储

## Status

Accepted

## Context

当前 `LocalStorageServiceImpl` 将文件写入本地磁盘，存在以下问题：
1. 进程重启后不可持久化（非持久卷场景）
2. 无法横向扩展（不同实例文件不互通）
3. 前端通过 localhost URL 访问，生产环境不可用

候选方案：阿里云 OSS、腾讯云 COS、Supabase Storage（已用 Supabase PostgreSQL）

## Decision

使用 Supabase Storage，理由：
- 与数据库在同一 Supabase 项目，统一管理
- REST API 集成简单（RestTemplate），无需额外 SDK
- 公开 bucket 返回可直接渲染的 URL，前端无需代理
- 通过 `@ConditionalOnProperty` 保留 `LocalStorageServiceImpl` 用于 dev 测试

## Consequences

- 新增 `SupabaseStorageServiceImpl`，移除 `exists()` 方法
- 新增 `deleteByUrl()` 方法，过期清理时一并删除存储文件
- `LocalStorageServiceImpl` 仅在 `app.storage.type=local` 时启用
- 需配置 `SUPABASE_SERVICE_ROLE_KEY` 环境变量
