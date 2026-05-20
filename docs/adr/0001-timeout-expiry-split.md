# ADR 0001: Timeout 与 Expiry 拆分

## Status

Accepted

## Context

原设计中 `app.task.expire-hours` 同时用于两个目的：
1. 判断 PROCESSING 任务是否超时（应标记 FAILED）
2. 判断 SUCCESS/FAILED 任务是否过期（应清理/不可查询）

混用导致：prod 环境设 24h，PROCESSING 任务要等 24h 才被清理；dev 环境设 1h，已完成任务 1h 后就不可查。

## Decision

拆分为两个独立配置：
- `app.task.timeout-hours`：PROCESSING 任务最大存活时间（prod: 1h, dev: 30min）
- `app.task.expiry-hours`：已完成任务保留时间（prod: 24h, dev: 1h）

超时判定锚点为 `createdTime`（非 heartbeat），理由：Seedream 正常耗时 5-60s，1h timeout 安全裕量 60x，heartbeat 带来的并发复杂度不值得。

## Consequences

- PROCESSING 任务最多挂 timeout-hours 即被清理，不再等 24h
- 已完成任务保留 expiry-hours 供用户查询
- 无需 heartbeat 线程、无并发写入问题
- 启动时额外清理进程重启前的残留 PROCESSING 任务
