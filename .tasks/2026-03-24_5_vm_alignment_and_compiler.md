﻿# 任务背景
当前工程运行时是“图解释器”，与 `bytecode-vm-spec.md` 的“可移植字节码 VM”目标存在语义断层。需要先让算法文档具备可编译语义，再修订字节码规范，最后落地一个 Java 参考 VM Core + 最小编译器，以便后续移植到 C/C++。

# 任务描述
1. 修订 `graph-logic-engine-impl.md`：补齐可编译语义（Basic Block / Wait Point / 确定性调度 / 归一化类别落到指令级）。
2. 修订 `bytecode-vm-spec.md`：明确 `WAIT_ACTION` 的 category 返回、增加 `WAIT_JOIN`、补齐 alignment 与预算规则。
3. 后端落地 Java VM Core 与最小编译器，并将执行路径切换为“编译 + VM 执行”，不提供 fallback。
4. 建立 Golden Trace 验证闭环。

# 实施清单
1. [x] 更新 `graph-logic-engine-impl.md`，新增“可编译语义层”章节并修正 Task/Join 的等待点语义。
2. [x] 更新 `bytecode-vm-spec.md`，修订 `WAIT_ACTION`、新增 `WAIT_JOIN`、补齐 alignment/relocation/budget。
3. [x] 新增 `com.example.graphlogic.vm`：Opcode/BytecodeVm/HostBinding/Program。
4. [x] 新增 `com.example.graphlogic.compiler`：最小编译器（Start/Task/Decision/End）。
5. [x] 建立 Golden Trace：新增 JUnit 用例验证 Task->onSuccess 的确定性路径。
6. [x] 切换 `TickExecutionEngine` 到“编译 + VM 执行”路径，遇到不支持图结构直接报错。

# 任务进度
- [2026-03-24 16:49:37] 创建任务文档与实施清单。状态：进行中。
- [2026-03-24 17:00:02] 完成两份规范修订；落地最小 Java VM Core 与最小编译器；`TickExecutionEngine` 切换到 VM 执行路径；后端 `mvn test` 通过。状态：完成。
