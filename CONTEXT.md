# CONTEXT.md — Photo Transform Service

## Glossary

### Task
一次证件照转化请求的完整生命周期。由用户提交触发，经过 PROCESSING → SUCCESS/FAILED 状态变迁。

### TaskId
任务的唯一标识，由服务层生成，作为数据库主键。非自增，非数据库生成。

### TaskIdGenerator
统一 ID 生成器，格式为前缀（PT/SD）加 16 位大写 UUID 子串。用于生成 TaskId 及 Seedream 请求 ID，消除重复的 ID 拼接逻辑。

### PhotoType
证件照类型标识，如 id-photo / passport / visa。请求时传入，存入任务实体，在 prompt 构建时路由到对应模板。当前仅 id-photo 模板，预留扩展。

### Timeout
PROCESSING 状态任务的最大存活时间。超时后由 cron 或启动清理标记为 FAILED。与 expiry 是不同概念。

### Expiry
已完成任务（SUCCESS/FAILED）的保留时间。过期后用户查询返回"已过期"，cron 可删除记录及关联文件。

### Event-Driven Async
事件驱动的异步任务调度模式。任务创建后发布仅含 taskId 的事件，监听器在独立线程池中消费并委托服务层处理。服务层从数据库加载实体，执行器不感知持久化细节，消除循环依赖。

### Seedream
火山引擎 AI 图像生成服务。同步阻塞调用，正常耗时 5-60 秒。

### GenerationCapability
Seedream 图像生成能力分类，共六种：文生图/单图生图/多图生图 × 单图/组图。根据参考图数量和生成模式自动识别，决定 SDK 调用参数。

### GenerationMode
生成模式，SINGLE（单图）或 SEQUENTIAL（组图）。影响 SDK 的 sequential_image_generation 参数，由系统根据请求内容自动选择。

### PARTIAL_SUCCESS
Seedream 组图模式下部分图片生成失败但仍有可用图片时的响应状态。系统将其等同于 SUCCESS 处理，选取首个有效图片作为结果。

### PromptTemplate
prompt 模板，包含 system（正面指令）和 negative（负面约束）两个分段。存储在配置文件的 prompt.templates 下，以 photoType 为 key。支持 {name}/{rgb} 占位符替换。

### ImageFetcher
图片下载组件，从远程 URL 获取图片字节数据。将网络 IO 从服务编排层分离到基础设施层，使服务层聚焦业务逻辑。

### Pooler
Supabase 的连接池代理层（transaction 模式）。应用侧通过 Shared Pooler 连接，无需自行管理 PG 连接复用。

### Supabase Storage
远程文件存储服务。替代本地文件系统，通过 REST API 上传/下载/删除文件。公开 bucket 返回可访问 URL，前端可直接渲染。

### Service Role Key
Supabase 服务端 API Key，拥有最高权限。仅在服务端使用，严禁暴露给前端。用于 Storage REST API 调用。
