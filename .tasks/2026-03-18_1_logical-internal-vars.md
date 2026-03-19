# 背景
文件名：2026-03-18_1_logical-internal-vars.md
创建于：2026-03-18_11:05:37
创建者：lxy\15957
主分支：main
任务分支：feature/logical-internal-vars
Yolo模式：Ask

# 任务描述
实现逻辑级内部变量（用户可自建/管理）并接入变量选择与转移逻辑配置：
1. 后端支持 Flow 内部变量定义（LogicalInternalVarDef）并随 Flowchart 传输
2. 前端增加内部变量管理面板，允许创建/编辑/删除变量
3. 前端 VariablePicker 支持 External / Internal / TaskResult / Constant 作为变量来源，并避免暴露系统变量（如 _stm_state）

# 项目概览
项目包含前端 React Flow 编辑器与后端 Spring Boot 模拟引擎。
技术文档定义了 External Snapshot / Internal State / Symbol Table 的运行时语义以及 Task/Decision/Join 的图层语义。
本任务聚焦在“用户可编辑的逻辑级内部变量”与“基于变量的转移逻辑配置”的编辑层数据结构与 UI 支撑，不实现完整 VM/字节码编译与执行。

⚠️ 警告：永远不要修改此部分 ⚠️
- 每个响应必须带模式声明：[MODE: ...]
- RESEARCH 只允许调查与提问，禁止提出方案/实施
- INNOVATE 只允许讨论可能性，禁止规划/写代码
- PLAN 只允许给出详尽技术规格与实施清单，禁止写代码
- EXECUTE 必须严格遵循已批准计划逐条实施，完成后更新任务进度并请求用户确认成功/不成功
- REVIEW 必须逐行核对计划与实施，标记任何偏差
⚠️ 警告：永远不要修改此部分 ⚠️

# 分析
当前外部变量以 schema 的方式由后端提供，并在前端 Decision 中以 bool 下拉选择支撑最小可用。
内部变量目前仅存在于文档语义中，前后端没有用户定义变量的结构与 UI，导致转移逻辑和参数绑定无法引用业务语义变量。
需要引入“Flow 级逻辑变量表”，并让 VariablePicker 以结构化 VariableRef 的形式引用 external/internal/taskResult/constant，避免魔法字符串。

# 提议的解决方案
- 后端：为 Flowchart 增加 internalVars 字段，类型为 LogicalInternalVarDef 列表
- 前端：新增 VariablePanel 管理 internalVars，新增 VariablePicker 支撑变量引用，Decision 配置改为基于 VariablePicker 的结构化条件

#实施清单
1. 后端新增 LogicalInternalVarDef，并在 Flowchart 中加入 internalVars 字段
2. 前端 types.ts 增加 LogicalInternalVarDef 与 VariableRef/DecisionCondition 结构，并在 Flowchart 类型中加入 internalVars
3. 前端新增 VariablePanel 组件，实现内部变量增删改（name/type/initial/description）
4. 前端新增 VariablePicker 组件，实现 external/internal/taskResult/constant 变量来源选择，其中 internal 仅来自用户变量列表
5. 前端更新 App：维护 internalVars 状态，将 VariablePanel 加入右侧面板，序列化时携带 internalVars
6. 前端更新 NodeInspector：Decision 配置改用 VariablePicker 构造结构化 condition，并接入 internalVars
7. 运行后端与前端构建验证，并进行基础手工验证

# 当前执行步骤："7. 运行后端与前端构建验证，并进行基础手工验证"

# 任务进度
[带时间戳的变更历史]
⚠️ 在写入时间前，你需要运行获取时间的命令，来获取当前的正确时间 ⚠️

2026-03-18_11:09:38
- 已修改：
  - c:\Code\RobotProj\GraphLogicEngine\backend\src\main\java\com\example\graphlogic\schema\LogicalInternalVarDef.java
  - c:\Code\RobotProj\GraphLogicEngine\backend\src\main\java\com\example\graphlogic\model\Flowchart.java
  - c:\Code\RobotProj\GraphLogicEngine\frontend\src\types.ts
  - c:\Code\RobotProj\GraphLogicEngine\frontend\src\VariablePanel.tsx
  - c:\Code\RobotProj\GraphLogicEngine\frontend\src\VariablePicker.tsx
  - c:\Code\RobotProj\GraphLogicEngine\frontend\src\App.tsx
  - c:\Code\RobotProj\GraphLogicEngine\frontend\src\NodeInspector.tsx
- 更改：
  - 后端 Flowchart 支持携带用户定义 internalVars
  - 前端新增内部变量管理面板（Internal Vars）
  - 前端新增 VariablePicker，Decision 条件改为结构化变量选择器 + 操作符 + 常量
  - 前端执行请求携带 internalVars
- 原因：
  - 引入用户可自建的逻辑级内部变量，并在 Decision 配置中可引用 external/internal/taskResult/constant，避免魔法字符串
- 阻碍因素：无
- 状态：未确认

# 最终审查
[完成后的总结]

