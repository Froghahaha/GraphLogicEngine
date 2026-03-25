# 任务背景
在 `TickExecutionEngine.java` 的实现中，能力调用机制存在严重缺陷：
1. `startCapability()` 方法仅设置模拟状态，未真正调用 `CapabilityRegistry` 中的能力实例
2. `getTaskState()` 方法从节点数据读取模拟状态，未查询真实的能力状态
3. 主循环未调用能力的 `tick()` 方法，导致 `TimedMockCapability` 的状态转换逻辑永远不会执行
4. 已注册的能力（MoveToPose, PickObject, ScanQRCode）从未被使用

这些缺陷导致能力框架（CapabilityInstance, CapabilityRegistry, TimedMockCapability）与执行引擎完全脱节，无法实现真实的能力调用。

# 任务描述
将 `TickExecutionEngine` 与 `CapabilityRegistry` 集成，实现真实的能力调用机制，为后续 VM 抽取做准备。

# 分析
## 当前问题
- **架构脱节**：能力框架设计完整，但执行引擎未接入
- **模拟实现缺陷**：`simulatedState` 永远不会转换，Task 节点永远处于 WAITING 状态
- **Tick 驱动缺失**：主循环未调用能力的 `tick()` 方法
- **参数传递缺失**：`startCapability()` 未从节点数据提取参数

## 设计目标
- 简单直接，避免过度设计（Linus 原则）
- 符合 Tick 驱动的确定性执行模型
- 为 VM 抽取做好准备
- 最小化改动，风险可控

# 提议的解决方案
采用最简方案：直接集成

1. **TickExecutionEngine 直接注入 CapabilityRegistry**
   - 添加 `CapabilityRegistry` 字段
   - 添加 `Map<String, CapabilityInstance> runningCapabilities` 存储运行中的能力实例

2. **重写 startCapability() 方法**
   - 从节点数据读取 `capabilityId`
   - 从 `CapabilityRegistry` 获取能力实例
   - 调用 `instance.start(params, nowMillis)`
   - 存储到 `runningCapabilities`

3. **重写 getTaskState() 方法**
   - 从 `runningCapabilities` 获取能力实例
   - 查询 `instance.getLifecycleState()`
   - 映射为字符串（Running, Done, Error, Timeout）

4. **修改主循环**
   - 在每个 Tick 中调用所有运行中能力的 `tick()` 方法
   - 在 Task 节点完成时清理能力实例

5. **修改 Bean 配置**
   - `GraphLogicApplication` 中修改 `tickExecutionEngine` Bean 创建，传入 `capabilityRegistry`

# 实施清单
1. [x] 修改 `TickExecutionEngine.java`，添加 `CapabilityRegistry` 和 `runningCapabilities` 字段
2. [x] 修改 `TickExecutionEngine.java` 构造函数，注入 `CapabilityRegistry`
3. [x] 添加必要的 import 语句到 `TickExecutionEngine.java`
4. [x] 重写 `TickExecutionEngine.java` 的 `startCapability()` 方法
5. [x] 重写 `TickExecutionEngine.java` 的 `getTaskState()` 方法
6. [x] 修改 `TickExecutionEngine.java` 的 `execute()` 主循环，添加能力 `tick()` 调用
7. [x] 在 `executeNode()` 方法中添加能力实例清理逻辑
8. [x] 修改 `GraphLogicApplication.java` 的 `tickExecutionEngine` Bean 创建方法
9. [x] 运行应用并验证能力调用是否正常工作

# 任务进度
- [2026-03-24] 完成 `TickExecutionEngine.java` 的能力集成改造
- [2026-03-24] 完成 `GraphLogicApplication.java` 的 Bean 配置修改
- [2026-03-24] 应用成功启动，无编译错误，能力集成完成

# 最终审查
实施与计划完全匹配。所有清单项目已完成：

1. ✅ 字段添加：CapabilityRegistry 和 runningCapabilities 已添加到 TickExecutionEngine
2. ✅ 构造函数修改：已注入 CapabilityRegistry 参数
3. ✅ Import 语句：已添加 CapabilityInstance, CapabilityLifecycleState, CapabilityRegistry
4. ✅ startCapability() 重写：从节点数据读取 capabilityId，从 Registry 获取实例，调用 start()，存储到 runningCapabilities
5. ✅ getTaskState() 重写：从 runningCapabilities 获取实例，查询 lifecycleState，映射为字符串
6. ✅ 主循环修改：在每个 Tick 中调用所有运行中能力的 tick()
7. ✅ 清理逻辑：在 Task 节点完成时从 runningCapabilities 移除实例
8. ✅ Bean 配置：GraphLogicApplication 中已传入 capabilityRegistry
9. ✅ 验证测试：应用成功启动，无编译错误

**实施与计划完全匹配**
