# 任务背景
在 `TickExecutionEngine.java` 的早期实现中，主循环存在硬编码逻辑（如强制将执行中的节点设为 `ACTIVE` 或 `COMPLETED`），这严重背离了 `graph-logic-engine-impl.md` 中关于异步节点（Task, Join）跨 Tick 执行的语义。此外，节点状态未遵循双缓冲机制，存在确定性隐患。

# 任务描述
重构 `TickExecutionEngine.java` 的调度算法，引入节点状态双缓冲，并将状态流转的控制权从主循环下放到节点逻辑中，确保异步节点能够正确地在多个 Tick 之间维持状态并自调度。

# 实施清单
1. [x] **重构主循环**：引入 `nodeStatesCurrent` 和 `nodeStatesNext`，实现 Tick 结束时的原子交换。
2. [x] **消除硬编码干扰**：移除主循环中所有武断的状态赋值，状态变更完全由 `executeNode` 决定。
3. [x] **完善节点执行协议**：
    - `Task 节点`：实现 `INACTIVE -> WAITING -> COMPLETED` 的完整流转，并在 `WAITING` 期间通过 `nextActiveNodes.add(nodeId)` 实现自调度。
    - `Join 节点`：基于 `nodeStatesCurrent` 快照检查汇合条件，未满足时自动挂起并在下一 Tick 重试。
    - `Decision 节点`：实现瞬时评估与分支激活。
4. [x] **代码瘦身**：移除冗余的辅助方法，将分发逻辑收拢至 `executeNode`，符合 Linus 风格的极简设计。

# 任务进度
- [2026-03-24] 完成 `TickExecutionEngine.java` 的深度重构。现在的引擎已经是一个真正的、符合确定性双缓冲规范的 Tick 驱动状态机调度器。
- [2026-03-24] 验证了异步 Task 节点的轮询机制，确保其在 WAITING 状态下不会被重复触发 startCapability。
