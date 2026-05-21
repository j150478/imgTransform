---
name: zoom-out
description: Tell the agent to zoom out and give broader context or a higher-level perspective. Use when you're unfamiliar with a section of code or need to understand how it fits into the bigger picture.
disable-model-invocation: true
---

I don't know this area of code well. Go up a layer of abstraction. Give me a map of all the relevant modules and callers, using the project's domain glossary vocabulary.

## Execution

**Step 1 — Get graph context**: call `get_minimal_context(task="<user's topic>")` to orient.

**Step 2 — Architecture view**: call `get_architecture_overview` for community structure and cross-community coupling warnings.

**Step 3 — Drill into relevant communities**: use `list_communities` → `get_community` on communities relevant to the user's question.

**Step 4 — Trace relationships**: use `query_graph` with `callers_of` / `callees_of` / `imports_of` on key nodes to map the dependency graph.

**Step 5 — Execution flows**: use `list_flows` and `get_flow` on flows that touch the relevant area.

**Step 6 — Answer**: synthesize into a map — modules → call chains → data flow — using project glossary terms from CONTEXT.md.
