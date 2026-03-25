# 任务背景
在目前的实现中，前后端关于“能力（Capability）”的定义是割裂的。后端在 `CapabilityRegistry` 中注册能力，而前端在 `nodeSchemas.ts` 中硬编码能力列表。这导致前端无法感知后端能力的参数需求、结果码等元数据，违反了 `graph-logic-engine-impl.md` 中关于“能力契约模型”的设计精神。

# 任务描述
实现“基于元数据驱动的能力集成（Metadata-Driven Capability Integration）”。后端建立能力契约模型并提供 API，前端消费该 API 动态生成能力选择和参数配置界面。

# 实施清单
1. [x] **后端：建立契约模型**
   - 创建了 `InputParamDef`, `ResultCodeDef`, `CapabilityMetadata` record 类。
   - 扩展了 `CapabilityInstance` 接口，增加 `getMetadata()` 方法。
   - 更新了 `TimedMockCapability`，在 `GraphLogicApplication` 注册时注入了真实的元数据（参数、结果码、推荐超时等）。
2. [x] **后端：暴露元数据 API**
   - 在 `VariableSchemaController` 中新增了 `GET /api/schema/capabilities` 接口，动态拉取所有已注册能力的契约。
3. [x] **前端：契约驱动的 UI**
   - 在 `types.ts` 中对齐了 `CapabilityMetadata` 相关类型。
   - 在 `App.tsx` 的 `useEffect` 中增加对 `/api/schema/capabilities` 的请求并下发。
   - 重构了 `NodeInspector.tsx`：移除了对 `capabilityOptions` 的硬编码引用。现在 Task 节点的下拉列表从 `capabilities` 动态生成。
   - **智能联动**：在选中某个能力时，自动将其 `recommendedTimeoutMs` 回填到 Timeout 输入框中。
   - **动态参数面板**：根据能力契约中的 `inputs` 数组，动态渲染参数输入框，并将数据保存到 `data.capabilityParams` 中。
4. [x] **清理**
   - 删除了 `nodeSchemas.ts` 中的 `capabilityOptions` 硬编码。

# 任务进度
- [2026-03-24] 实施完成。前后端能力模型已完全解耦并由元数据驱动，完美对齐了架构文档规范。状态：完成。
