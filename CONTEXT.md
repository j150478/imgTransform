# CONTEXT.md — Photo Transform Service

## Glossary

### Task
一次证件照转化请求的完整生命周期。由用户提交触发，经过 PROCESSING → SUCCESS/FAILED 状态变迁。

### TaskId
任务的唯一标识，由服务层生成，作为数据库主键。非自增，非数据库生成。

### Timeout
PROCESSING 状态任务的最大存活时间。超时后由 cron 或启动清理标记为 FAILED。与 expiry 是不同概念。

### Expiry
已完成任务（SUCCESS/FAILED）的保留时间。过期后用户查询返回"已过期"，cron 可删除记录及关联文件。

### Seedream
火山引擎 AI 图像生成服务。同步阻塞调用，正常耗时 5-60 秒。

### Pooler
Supabase 的连接池代理层（transaction 模式）。应用侧通过 Shared Pooler 连接，无需自行管理 PG 连接复用。

### Supabase Storage
远程文件存储服务。替代本地文件系统，通过 REST API 上传/下载/删除文件。公开 bucket 返回可访问 URL，前端可直接渲染。

### Service Role Key
Supabase 服务端 API Key，拥有最高权限。仅在服务端使用，严禁暴露给前端。用于 Storage REST API 调用。
