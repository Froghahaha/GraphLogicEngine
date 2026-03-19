# Graph Logic Engine Quick Start

面向希望**快速在本地跑通 DEMO** 的同学，下面分别给出 **Windows** 和 **Linux** 环境下的启动步骤。

当前项目包含两部分：
- Backend：Spring Boot（Java 17，Maven），提供 HTTP + WebSocket 接口
- Frontend：React + Vite，提供可视化 Flow Editor

> 约定：下面所有路径都以项目根目录 `c:\Code\RobotProj\GraphLogicEngine` / `/path/to/GraphLogicEngine` 为例，请根据你的实际路径替换。

---

## 1. 环境准备

### 1.1 通用要求
- Git（任意较新版本）
- Node.js 18+（建议安装 LTS 版本）
- Java 17 JDK
- Maven 3.8+（当前 backend 未提交 Maven Wrapper，需要系统安装 Maven）

### 1.2 Windows
1. 安装 Java 17  
   - 可使用 Microsoft OpenJDK 或 Eclipse Temurin，安装后确认：
   - 在 PowerShell 中执行：

     ```powershell
     java -version
     mvn -version
     node -v
     npm -v
     ```

2. 克隆仓库并进入目录：

   ```powershell
   git clone <your-repo-url> GraphLogicEngine
   cd GraphLogicEngine
   ```

### 1.3 Linux
以 Debian/Ubuntu 系为例（其他发行版请用等价命令）：

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven nodejs npm git
```

安装完成后确认：

```bash
java -version
mvn -version
node -v
npm -v
```

克隆仓库：

```bash
git clone <your-repo-url> GraphLogicEngine
cd GraphLogicEngine
```

---

## 2. 启动 Backend（Spring Boot）

Backend 代码在 [backend](file:///c:/Code/RobotProj/GraphLogicEngine/backend) 目录，使用 Spring Boot 3.2 + Java 17。

### 2.1 使用 Maven 启动（推荐开发模式）

**Windows / Linux 通用：**

```bash
cd backend
mvn spring-boot:run
```

- 首次运行会下载依赖，时间略长。
- 启动成功后，控制台会看到类似日志：
  - `Started GraphLogicApplication in ... seconds`
- 默认端口为 `8080`，提供接口：
  - `POST http://localhost:8080/api/flow/execute`：执行前端编辑的流程
  - WebSocket 端点：`ws://localhost:8080/ws`（由 `WebSocketConfig` 配置）

### 2.2 打包为可执行 JAR（可选）

```bash
cd backend
mvn clean package
```

成功后会在 `backend/target` 下生成类似：
- `graph-logic-engine-0.0.1-SNAPSHOT.jar`

运行：

```bash
cd backend/target
java -jar graph-logic-engine-0.0.1-SNAPSHOT.jar
```

---

## 3. 启动 Frontend（React + Vite）

Frontend 代码在 [frontend](file:///c:/Code/RobotProj/GraphLogicEngine/frontend) 目录，使用 Vite + React + TypeScript。

### 3.1 安装依赖

**Windows / Linux 通用：**

```bash
cd frontend
npm install
```

### 3.2 启动开发服务器

```bash
cd frontend
npm run dev
```

Vite 默认会在 `http://localhost:5173` 启动开发服务器，终端输出类似：

```text
  VITE v7.x  ready in ...

  ➜  Local:   http://localhost:5173/
```

在浏览器打开该地址，即可看到 Flow Editor 页面。

> 确保 backend 已经在 `http://localhost:8080` 启动，否则前端在执行流程 / 连接 WebSocket 时会报错。

---

## 4. 典型开发工作流

1. 终端 A：启动 backend

   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. 终端 B：启动 frontend

   ```bash
   cd frontend
   npm install   # 首次
   npm run dev
   ```

3. 浏览器访问 `http://localhost:5173`：
   - 在画布中编辑 Flow；
   - 点击页面中的“执行”按钮（若有）；
   - 后端会通过 `/api/flow/execute` 接收 Flowchart；
   - WebSocket 将节点状态（ACTIVE/COMPLETED 等）推送回前端。

---

## 5. 常见问题排查

### 5.1 Backend 无法启动
- 检查 Java 版本是否为 17：

  ```bash
  java -version
  ```

- 检查端口 8080 是否已被占用：
  - Windows：使用 `netstat -ano | findstr 8080`
  - Linux：使用 `ss -ltnp | grep 8080`

### 5.2 Frontend 连接不到 WebSocket
- 确认 backend 已启动；
- 确认浏览器控制台中 WebSocket URL 是否为 `ws://localhost:8080/ws`（参考 [WebSocketConfig.java](file:///c:/Code/RobotProj/GraphLogicEngine/backend/src/main/java/com/example/graphlogic/config/WebSocketConfig.java)）。

### 5.3 依赖安装失败（前端）
- 尝试切换国内 npm 源或使用 pnpm/yarn，但保持 `package-lock.json` 与 `npm` 的一致性优先。

---

## 6. 下一步：对接 Bytecode VM / Graph Logic Engine

当前 backend 中的 `StateMachineEngine` 只是一个**模拟执行器**，通过 `Thread.sleep` 来模拟节点执行时间，并通过 WebSocket 推送节点状态。

要与 `.docs` 中定义的 **Bytecode VM Runtime** 和 **Graph Logic Engine** 规范对齐，下一步可以考虑：
- 在 backend 中新增编译服务，将前端的 Flow 图编译为 Program Package（二进制或 JSON 中间格式）；
- 扩展或替换 `StateMachineEngine`，调用真实的 VM（可以是嵌入式设备上的远程 VM，或本地 Java 版仿真 VM）；
- 引入 Tick 概念和 Golden Trace 测试，使 Web 端 Demo 与真实运行时行为更一致。

本 quick_start 只聚焦于“如何在本地把 Demo 跑起来”，更深入的 VM/字节码细节请参考：
- [.docs/bytecode-vm-spec.md](file:///c:/Code/RobotProj/GraphLogicEngine/.docs/bytecode-vm-spec.md)
- [.docs/graph-logic-engine-impl.md](file:///c:/Code/RobotProj/GraphLogicEngine/.docs/graph-logic-engine-impl.md)
