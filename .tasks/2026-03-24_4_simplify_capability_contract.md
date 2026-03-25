# 任务背景
之前的“能力契约（Capability Contract）”实现过于复杂，为每个能力定义详尽的参数和结果码元数据。这导致了逻辑编排层与具体业务语义的深度耦合，不符合“逻辑引擎不应感知业务细节”的设计原则，且代码显得臃肿。

# 任务描述
重构能力契约，回归极简主义。通过“语义归一化”和“参数扁平化”，使引擎只关注通用的执行结果类别（SUCCESS, RETRY, ABORT）和通用的数据载荷（Payload），从而实现逻辑与业务的彻底解耦。

# 实施清单
1. [x] **后端：简化模型**
   - 删除了 `InputParamDef.java` 和 `ResultCodeDef.java`。
   - 精简了 `CapabilityMetadata.java`，仅保留 `id`, `version`, `description`, `recommendedTimeoutMs`。
   - 更新了 `GraphLogicApplication.java`，移除了上百行的冗余元数据定义。
2. [x] **后端：归一化语义流转**
   - 重构了 `TickExecutionEngine.java` 中的 Task 处理逻辑。
   - **结果映射**：
     - `SUCCESS` -> `onSuccess`
     - `RECOVERABLE_ERROR` -> `onRetry`
     - `FATAL_ERROR` -> `onAbort`
     - `TIMEOUT` -> `onTimeout`
   - 引擎不再关心具体的业务 Code（如 `NO_OBJECT`），只根据结果的性质决定流转方向。
3. [x] **前端：极简交互**
   - 更新了 `types.ts` 中的元数据接口。
   - 更新了 `nodeSchemas.ts`，将 Task 的出边角色统一为标准化的四种类别。
   - 重构了 `NodeInspector.tsx`，将复杂的动态表单替换为通用的 **Payload (JSON)** 编辑器，实现了参数的自由透传。
4. [x] **代码清理**
   - 移除了所有冗余的元数据解析逻辑和辅助方法。

# 任务进度
- [2026-03-24] 实施完成。系统回归到 Linus 风格的极简设计：边界清晰，逻辑通用，业务数据透明透传。状态：完成。
