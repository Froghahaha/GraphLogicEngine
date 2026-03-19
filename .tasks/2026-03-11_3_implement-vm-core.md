# 背景
文件名：2026-03-11_3_implement-vm-core.md
创建于：2026-03-11_10:24:30
创建者：Trae
主分支：main
任务分支：feature/vm-core-implementation
Yolo模式：Ask

# 任务描述
在 Java 后端实现完整的 Bytecode VM Runtime 核心逻辑，包括：
1. 固定 Tick 驱动的主循环
2. 双缓冲状态管理 (Internal State)
3. 外部快照隔离 (External Snapshot)
4. 能力契约与 mock 执行
5. Task/Decision/Join 节点的语义实现

# 项目概览
当前项目包含前后端，后端目前仅是一个简单的状态机模拟器。
目标是将后端改造为符合 `.docs/bytecode-vm-spec.md` 和 `.docs/graph-logic-engine-impl.md` 规范的参考实现。

⚠️ 警告：永远不要修改此部分 ⚠️
[此部分应包含核心RIPER-5协议规则的摘要，确保它们可以在整个执行过程中被引用]
⚠️ 警告：永远不要修改此部分 ⚠️

# 分析
核心需求是将规范文档中的概念映射到 Java 代码中：
- `Tick`: 使用 ScheduledExecutorService 模拟 100Hz (或调试频率)
- `Double Buffer`: 使用 `Map<String, Object>` 的两个实例 `current` 和 `next`
- `External Snapshot`: 不可变对象，包含 IO/System 状态
- `Capability`: 定义接口和 Mock 实现，支持异步生命周期
- `Node Execution`: 实现 Task/Decision/Join 的 Tick 级语义

# 提议的解决方案
1. 创建 `com.example.graphlogic.vm` 包，隔离 VM 核心逻辑
2. 定义 `VmContext` 包含 `InternalState`, `ExternalSnapshot`, `Frame`
3. 实现 `TickEngine` 负责主循环
4. 实现 `CapabilityManager` 管理能力注册与 Mock 执行
5. 重构 `StateMachineEngine` 使用新的 VM 核心

# 实施清单
1. 创建 VM 核心数据结构 (InternalState, ExternalSnapshot, VmContext)
2. 实现能力契约接口与 Mock 能力 (Capability, MockCapability)
3. 实现 Tick 驱动引擎 (TickEngine) 与双缓冲逻辑
4. 实现节点语义执行器 (NodeExecutor: Task, Decision, Join)
5. 集成到 Spring Boot Service 并暴露 WebSocket 状态
6. 编写单元测试验证 Tick 时序与状态一致性

# 当前执行步骤："[步骤编号和名称]"
- 例如："2. 创建任务文件"

# 任务进度
[带时间戳的变更历史]
⚠️ 在写入时间前，你需要运行获取时间的命令，来获取当前的正确时间 ⚠️

# 最终审查
[完成后的总结]
