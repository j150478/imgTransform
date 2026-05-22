# CLAUDE.md

## 项目概述

这是一个基于 Spring Boot 的证件照转化后端服务（Photo Transform Service），集成了 Seedream API 提供 AI 图像生成和转换功能。

## Quick Start

```bash
mvn clean compile       # 编译
mvn spring-boot:run     # 开发模式（端口 8081，H2 + 本地存储）
mvn test                # 运行所有测试
```

## Testing

```bash
mvn test -Dtest='PhotoTransformServiceImplTest,PhotoTransformControllerTest'  # 核心测试（无需外部服务）
mvn test -Dtest=PhotoTransformControllerRealApiTest  # 真实 Seedream API（test profile）
mvn test -Dtest=SupabaseStorageIntegrationTest        # Supabase Storage（prod profile）
```

- 测试用 `@BeforeAll` + `@TestInstance(PER_CLASS)` 避免 unique 约束冲突
- controller 集成测试需先创建 User + UserQuota，通过 JwtUtil 生成 authToken，所有请求带 `Authorization: Bearer {token}`
- prod 环境测试用时间戳生成唯一手机号，避免持久化 DB 数据残留

## Agent skills

### Issue tracker

Issues tracked as local markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Using default five-role vocabulary (needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context repo — one `CONTEXT.md` + `docs/adr/` at repo root. See `docs/agents/domain.md`.

<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
| ------ | ---------- |
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.

## Development Workflow

- commit/push 调用 `review-commit-push` agent，不在主会话操作
- 大功能拆分为独立 task，用 worktree 隔离并行执行，选合适模型（haiku 做 CRUD，sonnet 做核心逻辑）

## Gotchas

- code-review-graph MCP 通过 uvx 从 PyPI 下载运行，需终端代理可用，否则 MCP 启动报 `-32000: Connection closed`
- settings.json 配置了 Bash deny 黑名单（rm -r*、git reset --hard、git push --force、sudo、curl/wget | sh 管道执行等），详见 `.claude/settings.json`
- Redis 通过 brew 安装（`brew services start redis`），测试依赖 localhost:6379
- uv 缓存（`~/.cache/uv/`）可能膨胀到 4G+，定期 `uv cache clean` 清理
