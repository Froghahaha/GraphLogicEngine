# 任务背景
前端界面中，用户在编辑 Decision 节点等多分支节点时，难以直接选中特定连线进行配置，且报错 "No edge selected" 引起了混淆。
后端请求外部变量结构时出现 CORS 跨域错误。
后端的 `TickExecutionEngine.java` 早期实现在状态转移上采用了 `findFirst` 的简单粗暴方案，现在虽然部分更新为了 `selectNextNodeId` 逻辑，但评估条件并未支持所有的变量源（如 `internal` 和 `taskResult`）。

# 任务描述
1. 修复后端 CORS 跨域问题。
2. 修复前端 `NodeInspector` 连线配置 UI，使得目标节点明确可读（显示 label 而非单纯 id）。
3. 完善后端 `TickExecutionEngine` 状态转移逻辑，全面支持外部变量、内部变量以及任务状态的结果作为条件进行分支评估。

# 实施清单
1. [x] 创建 `WebMvcConfig.java` 实现全局 CORS 跨域支持，修复 `localhost:8080/api/schema/external-vars` 请求报错。
2. [x] 在 `App.tsx` 和 `NodeInspector.tsx` 中注入 `allNodes` 列表，使 Outgoing Edges 列表能够展示目标节点的 `label` 增强可读性。
3. [x] 在 `TickExecutionEngine.java` 中重构 `evalDecisionCondition` 方法以支持 `internal` 和 `taskResult` 变量解析。
4. [x] 将 `internalVarsCurrent` 等上下文透传到 `selectNextNodeId` 和 `executeNode`，完成 Linus 风格的极简、无冗余边界的 Tick 驱动状态转移。

# 任务进度
- [2026-03-24] 实施完成上述所有清单内容，修改了 `NodeInspector.tsx` 和 `App.tsx` 解决前端 UI 混淆，增加 `WebMvcConfig` 解决跨域，修改 `TickExecutionEngine` 全面支持内部变量与任务结果作为状态流转判断条件。状态：完成。
