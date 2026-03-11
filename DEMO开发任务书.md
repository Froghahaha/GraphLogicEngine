# 基于图的机器人编程引擎开发任务书

## 一、项目概述

### 1.1 项目目标
开发一个基于状态机模型的流程图执行引擎，将图形化编程逻辑转化为机器人可执行指令，同时提供实时状态监控和可视化反馈。

### 1.2 核心概念
- **节点（Node）**：流程图中的基本执行单元
- **边（Edge）**：节点间的连接，代表执行顺序或条件转移
- **状态（State）**：节点在执行过程中的状态
- **事件（Event）**：触发状态转移的外部或内部信号
- **上下文（Context）**：执行过程中的变量和环境信息

## 二、整体架构设计

```
┌─────────────────────────────────────────────────────┐
│                    前端界面层                         │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐   │
│  │ 流程图编辑器 │  │ 实时监控器 │  │ 控制面板    │   │
│  └────────────┘  └────────────┘  └────────────┘   │
└────────────────────────┬───────────────────────────┘
                         │ WebSocket/HTTP
                         ▼
┌─────────────────────────────────────────────────────┐
│                   执行引擎层                          │
│  ┌─────────────────────────────────────────────┐   │
│  │              状态机执行引擎                    │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  │   │
│  │  │ 状态管理  │  │ 事件处理  │  │ 转移逻辑  │  │   │
│  │  └──────────┘  └──────────┘  └──────────┘  │   │
│  └─────────────────────────────────────────────┘   │
│  ┌──────────────┐  ┌──────────────┐               │
│  │ 变量管理器    │  │ 表达式求值器  │               │
│  └──────────────┘  └──────────────┘               │
└────────────────────────┬───────────────────────────┘
                         │ 调用/回调
                         ▼
┌─────────────────────────────────────────────────────┐
│                   执行器适配层                        │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐   │
│  │ 机器人适配器 │  │ IO适配器   │  │ 定时器适配器 │   │
│  └────────────┘  └────────────┘  └────────────┘   │
└────────────────────────┬───────────────────────────┘
                         │ 底层API
                         ▼
┌─────────────────────────────────────────────────────┐
│                   硬件控制层                          │
│               现有机器人控制系统                       │
└─────────────────────────────────────────────────────┘
```

## 三、详细开发任务分解

### 任务1：定义核心数据模型（1周）

#### 1.1 节点类型定义
```plaintext
节点基类 Node {
    id: 唯一标识符
    type: 节点类型枚举
    label: 显示标签
    position: {x, y} 在画布中的位置
    properties: Map<属性名, 属性值>
    status: 状态枚举 (INACTIVE, WAITING, EXECUTING, COMPLETED, ERROR)
    executionContext: 执行上下文
}

具体节点类型:
- StartNode: 开始节点（无输入，单输出）
- EndNode: 结束节点（单输入，无输出）
- ActionNode: 动作节点（单输入，单输出）
  - actionType: 动作类型枚举
  - parameters: 动作参数
  
- DecisionNode: 判断节点（单输入，多输出）
  - condition: 条件表达式
  - branches: [{condition, targetNodeId}]
  
- ParallelForkNode: 并行分支节点（单输入，多输出）
  - branchCount: 分支数量
  
- ParallelJoinNode: 并行合并节点（多输入，单输出）
  - requiredCount: 需要等待完成的数量
  
- LoopStartNode: 循环开始节点
  - loopType: 循环类型 (FOR, WHILE)
  - condition: 循环条件
  - maxIterations: 最大迭代次数
  
- LoopEndNode: 循环结束节点
  - loopStartId: 对应的循环开始节点ID
```

#### 1.2 边定义
```plaintext
边基类 Edge {
    id: 唯一标识符
    sourceNodeId: 源节点ID
    targetNodeId: 目标节点ID
    sourcePort: 源端口标识
    targetPort: 目标端口标识
    type: 边类型 (EXECUTION, DATA, CONDITION_TRUE, CONDITION_FALSE)
    condition: 可选，转移条件表达式
    priority: 优先级（用于多边选择）
}
```

#### 1.3 流程图定义
```plaintext
流程图类 Flowchart {
    id: 唯一标识符
    name: 流程图名称
    version: 版本号
    metadata: 元数据（作者、创建时间等）
    
    nodes: Map<节点ID, Node>  # 节点集合
    edges: List<Edge>         # 边集合
    
    variables: Map<变量名, 变量定义>
    inputs: Map<输入名, 输入定义>
    outputs: Map<输出名, 输出定义>
    
    # 图结构验证
    validate(): 验证结果 {
        // 1. 必须有且仅有一个开始节点
        // 2. 所有节点必须可达
        // 3. 不能有孤立的节点
        // 4. 循环必须正确闭合
        // 5. 并行分支和合并必须匹配
    }
}
```

### 任务2：状态机引擎实现（2周）

#### 2.1 状态机核心类
```plaintext
状态机引擎类 StateMachineEngine {
    # 状态管理
    - currentStates: Set<节点ID>           # 当前激活的节点
    - completedNodes: Set<节点ID>          # 已完成的节点
    - errorNodes: Set<节点ID>              # 出错的节点
    - waitingNodes: Map<节点ID, 等待条件>   # 等待条件的节点
    
    # 执行上下文
    - executionContext: ExecutionContext   # 变量和作用域
    - callStack: List<调用帧>               # 调用栈（用于子流程）
    
    # 事件系统
    - eventQueue: PriorityQueue<事件>       # 事件队列（按优先级）
    - eventHandlers: Map<事件类型, List<处理器>>
    
    # 定时器管理
    - activeTimers: Map<定时器ID, 定时器>
    
    # 主要方法
    + loadFlowchart(flowchart): 加载流程图
    + start(): 启动执行
    + pause(): 暂停执行
    + resume(): 恢复执行
    + stop(): 停止执行
    + step(): 单步执行
    
    + getCurrentStatus(): 获取当前状态
    + getActiveNodes(): 获取激活节点
    + getVariable(name): 获取变量值
    + setVariable(name, value): 设置变量值
    
    # 内部方法
    - processEvent(event): 处理单个事件
    - processEvents(): 处理事件队列
    - activateNode(nodeId): 激活节点
    - deactivateNode(nodeId): 停用节点
    - transition(fromNode, toNode, condition): 状态转移
}
```

#### 2.2 执行上下文
```plaintext
执行上下文类 ExecutionContext {
    # 变量作用域
    - globalScope: Map<变量名, 变量值>      # 全局变量
    - localScopes: Stack<Map<变量名, 变量值>> # 局部变量栈
    
    # 执行状态
    - executionId: 执行实例ID
    - startTime: 开始时间
    - currentNodeId: 当前节点ID（用于单步执行）
    - breakpoints: Set<节点ID>             # 断点集合
    
    # 历史记录
    - executionTrace: List<执行记录>       # 执行轨迹
    - variableHistory: Map<变量名, List<变更记录>> # 变量历史
    
    # 方法
    + pushScope(): 推入新的作用域
    + popScope(): 弹出作用域
    + setVariable(name, value, scope="local"): 设置变量
    + getVariable(name, scope="auto"): 获取变量
    + evaluateExpression(expression): 求值表达式
    + recordStep(nodeId, result): 记录执行步骤
}
```

#### 2.3 事件系统
```plaintext
事件类 Event {
    type: 事件类型枚举
    sourceId: 事件源ID（节点ID或系统）
    timestamp: 时间戳
    priority: 优先级（0-10，越高越优先）
    data: 事件数据（可变）
    
    事件类型：
    - NODE_STARTED: 节点开始执行
    - NODE_COMPLETED: 节点执行完成
    - NODE_ERROR: 节点执行错误
    - CONDITION_CHANGED: 条件变化
    - TIMER_EXPIRED: 定时器到期
    - USER_INTERRUPT: 用户中断
    - VARIABLE_CHANGED: 变量值变化
    - SYSTEM_ALERT: 系统警报
}

事件处理器接口 EventHandler {
    + canHandle(eventType): 布尔值
    + handle(event, context): 处理结果
}

具体处理器：
- NodeCompletionHandler: 处理节点完成事件
- ConditionHandler: 处理条件变化事件
- TimerHandler: 处理定时器事件
- ErrorHandler: 处理错误事件
```

### 任务3：节点执行器实现（2周）

#### 3.1 节点执行器基类
```plaintext
节点执行器接口 NodeExecutor {
    + supports(nodeType): 布尔值
    + execute(node, context): 执行结果
    + canExecute(node, context): 布尔值
    + getRequiredResources(node): 资源列表
}

执行结果类 ExecutionResult {
    status: 状态枚举 (SUCCESS, FAILURE, WAITING, SKIPPED)
    message: 结果消息
    data: 执行数据
    nextNodes: List<下一个节点ID>  # 指定下一步要执行的节点
    waitCondition: 可选，等待条件
}
```

#### 3.2 具体节点执行器

```plaintext
# 开始节点执行器
StartNodeExecutor 实现 NodeExecutor {
    execute(node, context) {
        // 开始节点总是立即完成
        返回 ExecutionResult {
            status: SUCCESS,
            nextNodes: [获取输出边指向的节点]
        }
    }
}

# 动作节点执行器
ActionNodeExecutor 实现 NodeExecutor {
    execute(node, context) {
        // 1. 解析动作参数
        actionType = node.properties.actionType
        parameters = 解析参数(node.properties.parameters, context)
        
        // 2. 创建动作执行器
        actionExecutor = ActionExecutorFactory.create(actionType)
        
        // 3. 执行动作（异步）
        future = actionExecutor.executeAsync(parameters)
        
        // 4. 设置回调
        future.then(结果 => {
            if (结果.success) {
                发送事件(NODE_COMPLETED, node.id, 结果)
            } else {
                发送事件(NODE_ERROR, node.id, 结果.error)
            }
        })
        
        // 5. 返回等待状态
        返回 ExecutionResult {
            status: WAITING,
            waitCondition: `action_${node.id}_completed`
        }
    }
}

# 判断节点执行器
DecisionNodeExecutor 实现 NodeExecutor {
    execute(node, context) {
        // 1. 评估条件
        condition = node.properties.condition
        conditionResult = context.evaluateExpression(condition)
        
        // 2. 记录决策
        context.recordDecision(node.id, conditionResult)
        
        // 3. 根据结果选择分支
        targetNode = 条件为真 ? 
                    node.properties.trueBranch : 
                    node.properties.falseBranch
        
        // 4. 立即完成并指定下一个节点
        返回 ExecutionResult {
            status: SUCCESS,
            nextNodes: [targetNode]
        }
    }
}

# 并行分支节点执行器
ParallelForkExecutor 实现 NodeExecutor {
    execute(node, context) {
        // 1. 获取所有输出边指向的节点
        targetNodes = 获取所有输出边目标节点(node.id)
        
        // 2. 创建并行执行上下文
        parallelContext = {
            id: 生成并行ID(),
            parentContext: context,
            branchCount: targetNodes.length,
            completedBranches: 0,
            results: Map<分支ID, 执行结果>
        }
        
        // 3. 激活所有分支
        for 每个 targetNode in targetNodes {
            分支ID = 生成分支ID()
            context.setVariable(`${node.id}_branch_${分支ID}`, "ACTIVE")
            发送事件(NODE_STARTED, targetNode, {branchId: 分支ID})
        }
        
        // 4. 节点立即完成（不等待分支）
        返回 ExecutionResult {
            status: SUCCESS
            // 注意：没有nextNodes，分支会独立执行
        }
    }
}
```

#### 3.3 动作执行器系统
```plaintext
动作执行器接口 ActionExecutor {
    + execute(parameters, context): Promise<执行结果>
    + cancel(): 取消执行
    + getProgress(): 执行进度
}

# 机器人移动动作
MoveToActionExecutor 实现 ActionExecutor {
    execute(parameters, context) {
        // 1. 验证参数
        目标位置 = parameters.target
        速度 = parameters.speed
        坐标系 = parameters.coordinateSystem || "world"
        
        // 2. 调用机器人API
        返回 robotController.moveToAsync(目标位置, 速度, 坐标系)
            .then(结果 => {
                // 3. 返回标准化结果
                return {
                    success: 结果.success,
                    data: {
                        actualPosition: 结果.position,
                        timeUsed: 结果.duration
                    },
                    message: 结果.message
                }
            })
            .catch(错误 => {
                return {
                    success: false,
                    error: 错误.message,
                    retryable: 错误.isRetryable
                }
            })
    }
}

# 夹爪控制动作
GripperActionExecutor 实现 ActionExecutor {
    execute(parameters, context) {
        action = parameters.action  // "open" 或 "close"
        force = parameters.force || 默认力
        
        // 调用机器人API
        return gripperControllerforce
    }
}

# 等待动作
WaitActionExecutor 实现 ActionExecutor {
    execute(parameters, context) {
        waitType = parameters.type  // "time", "condition", "signal"
        
        if (waitType == "time") {
            duration = parameters.duration
            
            // 设置定时器
            timerId = setTimeout(() => {
                解决Promise({success: true})
            }, duration)
            
            // 返回可取消的Promise
            return {
                promise: 定时器Promise,
                cancel: () => clearTimeout(timerId)
            }
        }
        else if (waitType == "condition") {
            condition = parameters.condition
            timeout = parameters.timeout || 60000  // 默认60秒超时
            
            // 等待条件满足
            return waitForCondition(condition, context, timeout)
        }
    }
}
```

### 任务4：表达式求值系统（1周）

#### 4.1 表达式语言设计
```plaintext
表达式语法：
- 字面量: 123, 3.14, "hello", true, false
- 变量引用: ${变量名}, ${对象.属性}
- 算术运算: +, -, *, /, %, ^
- 比较运算: ==, !=, >, <, >=, <=
- 逻辑运算: &&, ||, !
- 函数调用: 函数名(参数1, 参数2, ...)
- 三目运算: 条件 ? 真值 : 假值

预定义函数：
- 数学函数: sin(), cos(), abs(), round(), sqrt()
- 字符串函数: length(), substring(), contains()
- 数组函数: size(), get(), slice()
- 系统函数: now(), random(), format()
- 机器人函数: position(), velocity(), isMoving()
```

#### 4.2 求值器实现
```plaintext
表达式求值器类 ExpressionEvaluator {
    # 变量解析器
    - variableResolvers: List<变量解析器>
    
    # 函数注册表
    - functions: Map<函数名, 函数实现>
    
    # 方法
    + evaluate(expression, context): 求值结果
    + validate(expression): 验证表达式语法
    + getVariables(expression): 获取表达式中引用的变量
    
    # 内部方法
    - parseExpression(expression): 解析为AST
    - evaluateNode(astNode, context): 递归求值
    - resolveVariable(name, context): 解析变量值
    - callFunction(name, args, context): 调用函数
}

# AST节点类型
AST节点类 {
    type: 节点类型 (LITERAL, VARIABLE, BINARY_OP, UNARY_OP, CALL, CONDITIONAL)
    value: 节点值
    children: 子节点列表
}

# 变量解析器接口
VariableResolver {
    + canResolve(name): 布尔值
    + resolve(name, context): 变量值
}

具体解析器：
- ContextVariableResolver: 解析执行上下文中的变量
- SystemVariableResolver: 解析系统变量（时间、状态等）
- RobotVariableResolver: 解析机器人状态变量
- IOVariableResolver: 解析IO变量
```

### 任务5：错误处理和恢复机制（1周）

#### 5.1 错误分类
```plaintext
错误级别：
- FATAL: 致命错误，必须停止执行
- ERROR: 严重错误，需要处理
- WARNING: 警告，可继续执行
- INFO: 信息性消息

错误类型：
- SYNTAX_ERROR: 语法错误（流程图定义错误）
- RUNTIME_ERROR: 运行时错误（执行过程中出错）
- VALIDATION_ERROR: 验证错误（参数验证失败）
- TIMEOUT_ERROR: 超时错误
- RESOURCE_ERROR: 资源错误（资源不足或不可用）
- COMMUNICATION_ERROR: 通信错误
```

#### 5.2 错误处理器
```plaintext
错误处理器接口 ErrorHandler {
    + canHandle(error): 布尔值
    + handle(error, context): 处理结果
    + getRecoveryStrategy(error): 恢复策略
}

# 默认错误处理器
DefaultErrorHandler 实现 ErrorHandler {
    handle(error, context) {
        switch (error.level) {
            case FATAL:
                // 紧急停止
                context.emergencyStop()
                发送事件(SYSTEM_ALERT, "致命错误", error)
                break
                
            case ERROR:
                // 尝试恢复
                strategy = getRecoveryStrategy(error)
                applyRecovery(strategy, context)
                记录错误(error)
                break
                
            case WARNING:
                // 记录警告，继续执行
                记录警告(error)
                break
        }
    }
    
    getRecoveryStrategy(error) {
        // 根据错误类型和上下文决定恢复策略
        if (error.type == TIMEOUT_ERROR) {
            if (error.context.retryCount < 3) {
                return RETRY  // 重试
            } else {
                return SKIP_AND_CONTINUE  // 跳过并继续
            }
        }
        else if (error.type == RESOURCE_ERROR) {
            return WAIT_AND_RETRY  // 等待后重试
        }
        else {
            return STOP  // 停止执行
        }
    }
}
```

#### 5.3 恢复策略
```plaintext
恢复策略枚举：
- RETRY: 重试操作
- RETRY_WITH_BACKOFF: 带退避的重试
- SKIP_AND_CONTINUE: 跳过当前节点继续
- ROLLBACK_AND_RETRY: 回滚并重试
- CONTINUE_WITH_ALTERNATIVE: 使用替代方案继续
- PAUSE_AND_WAIT: 暂停并等待人工干预
- STOP: 停止执行
```

### 任务6：实时监控和通信（1周）

#### 6.1 状态上报机制
```plaintext
状态上报器类 StatusReporter {
    # 上报通道
    - channels: List<上报通道>
    
    # 上报内容
    - state: 系统状态
    - activeNodes: 激活节点列表
    - variables: 变量快照
    - executionTrace: 执行轨迹
    - errors: 错误列表
    
    # 方法
    + addChannel(channel): 添加上报通道
    + removeChannel(channel): 移除上报通道
    + reportStateChange(newState): 上报状态变化
    + reportNodeActivated(nodeId): 上报节点激活
    + reportNodeCompleted(nodeId, result): 上报节点完成
    + reportVariableChanged(name, value): 上报变量变化
    + reportError(error): 上报错误
    
    # 内部方法
    - createStatusMessage(): 创建状态消息
    - shouldReport(变化类型): 是否应该上报（频率控制）
    - compressData(数据): 压缩数据
}

# 上报通道接口
ReportChannel {
    + connect(): 连接
    + disconnect(): 断开连接
    + send(message): 发送消息
    + isConnected(): 是否已连接
}

具体通道：
- WebSocketChannel: WebSocket通道
- HTTPChannel: HTTP轮询通道
- FileChannel: 文件日志通道
- ConsoleChannel: 控制台输出通道
```

#### 6.2 监控数据格式
```plaintext
状态消息格式：
{
    "type": "status_update",
    "timestamp": "2024-01-01T10:30:00Z",
    "executionId": "exec_123456",
    "state": "RUNNING",  // IDLE, RUNNING, PAUSED, ERROR, COMPLETED
    "activeNodes": [
        {
            "nodeId": "node_1",
            "nodeType": "ACTION",
            "status": "EXECUTING",
            "startTime": "2024-01-01T10:29:55Z",
            "progress": 0.75  // 执行进度
        }
    ],
    "recentlyCompleted": ["node_0", "node_start"],
    "variables": {
        "robot.position.x": 100.5,
        "robot.position.y": 200.3,
        "sensor_1": true
    },
    "performance": {
        "executionTime": 15.3,  // 秒
        "nodesProcessed": 5,
        "averageNodeTime": 3.06
    }
}
```

### 任务7：测试和验证（2周）

#### 7.1 单元测试
```plaintext
测试用例分类：
1. 状态机引擎测试
   - 测试状态转移逻辑
   - 测试事件处理
   - 测试错误处理

2. 节点执行器测试
   - 测试各种节点类型的执行
   - 测试参数验证
   - 测试异常情况处理

3. 表达式求值测试
   - 测试基本表达式求值
   - 测试变量解析
   - 测试函数调用

4. 集成测试
   - 测试完整流程图执行
   - 测试与机器人控制系统的集成
   - 测试前端通信
```

#### 7.2 测试工具
```plaintext
# 模拟机器人控制器
MockRobotController 实现 RobotController {
    moveTo(position, speed) {
        // 模拟移动，可控制成功/失败
        if (模拟失败) {
            return Promise.reject(new Error("移动失败"))
        }
        
        // 模拟移动时间
        return new Promise(resolve => {
            setTimeout(() => {
                resolve({
                    success: true,
                    actualPosition: position,
                    timeUsed: 计算移动时间(position, speed)
                })
            }, 模拟时间)
        })
    }
}

# 测试流程图生成器
TestFlowchartGenerator {
    generateLinearFlow(count): 生成线性流程图
    generateBranchingFlow(): 生成分支流程图
    generateLoopingFlow(): 生成循环流程图
    generateParallelFlow(): 生成并行流程图
    generateComplexFlow(): 生成复杂组合流程图
}

# 自动化测试框架
FlowchartTestRunner {
    runTest(flowchart, expectedResults): 测试结果 {
        // 1. 加载流程图
        engine.loadFlowchart(flowchart)
        
        // 2. 设置模拟控制器
        engine.setRobotController(mockController)
        
        // 3. 执行测试
        engine.start()
        
        // 4. 收集执行结果
        results = 收集执行结果()
        
        // 5. 验证结果
        return 验证结果(results, expectedResults)
    }
}
```

## 四、开发里程碑

### 里程碑1：基础框架（3周）
- 完成数据模型定义
- 实现状态机引擎核心
- 实现基本节点执行器
- 通过单元测试

### 里程碑2：完整功能（4周）
- 实现所有节点类型
- 完成表达式求值系统
- 实现错误处理机制
- 实现基本监控功能
- 通过集成测试

### 里程碑3：性能优化（2周）
- 优化状态转移性能
- 实现状态压缩和持久化
- 优化内存使用
- 通过性能测试

### 里程碑4：工业级特性（3周）
- 实现断点调试
- 实现状态恢复
- 实现批量执行
- 通过压力测试

## 五、关键设计原则

### 5.1 可扩展性设计
- 使用插件架构，可以轻松添加新的节点类型
- 支持自定义函数和变量解析器
- 提供扩展点用于定制行为

### 5.2 可靠性设计
- 所有操作都有超时机制
- 实现原子性操作，避免部分失败
- 提供完整的错误恢复策略
- 支持状态持久化和恢复

### 5.3 性能设计
- 使用事件驱动，避免阻塞
- 实现状态缓存，减少重复计算
- 支持增量更新，减少数据传输
- 优化内存使用，避免内存泄漏

### 5.4 可维护性设计
- 清晰的模块划分
- 完整的日志记录
- 详细的错误信息
- 易于调试的接口

## 六、部署和运维

### 6.1 部署要求
- 运行环境：支持C++17的Linux系统
- 内存要求：至少512MB RAM
- 存储要求：至少100MB磁盘空间
- 网络要求：支持WebSocket通信

### 6.2 配置管理
- 支持配置文件定义节点类型
- 支持动态加载新节点
- 支持运行时常量配置

### 6.3 监控和日志
- 提供运行状态监控接口
- 支持多种日志级别
- 日志轮转和归档
- 性能指标收集

## 七、验收标准

### 7.1 功能验收
- [ ] 支持所有节点类型的创建和执行
- [ ] 支持完整的流程控制（顺序、分支、循环、并行）
- [ ] 支持变量和表达式求值
- [ ] 支持实时状态监控
- [ ] 支持错误处理和恢复
- [ ] 支持断点调试

### 7.2 性能验收
- [ ] 流程图加载时间 < 1秒（500节点）
- [ ] 状态转移延迟 < 100毫秒
- [ ] 支持并发执行10个以上流程
- [ ] 内存使用稳定，无内存泄漏

### 7.3 可靠性验收
- [ ] 7x24小时稳定运行
- [ ] 支持断电恢复
- [ ] 错误处理覆盖率 > 95%
- [ ] 所有测试用例通过率100%

## 八、风险管理

### 8.1 技术风险
- **风险**：状态机引擎复杂度高
- **缓解**：采用分阶段开发，先实现核心功能
- **风险**：与现有系统集成困难
- **缓解**：设计适配器模式，隔离变化

### 8.2 进度风险
- **风险**：开发时间估计不足
- **缓解**：采用敏捷开发，每2周可交付可用版本
- **风险**：需求变更
- **缓解**：保持架构灵活，支持扩展

### 8.3 质量风险
- **风险**：测试覆盖不全
- **缓解**：实施测试驱动开发，保持高测试覆盖率
- **风险**：性能问题
- **缓解**：早期进行性能测试，优化热点路径

---

**文档版本**：v2.0  
**最后更新**：2024年1月  
**预计工作量**：12-16人周  
**优先级**：P0（核心功能）  
**状态**：开发中