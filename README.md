# MakeUHealth 个人健康管理系统

Vue 前端与 Spring Boot 后端组成的个人健康管理系统，提供注册登录、个人档案、健康记录、饮食与每日热量目标、健身目标与周进度、网页内提醒及 DeepSeek 健康助手。支持 English 和简体中文界面，助手的新回答跟随所选语言。

项目仓库：[XiubaiZero/MakeUHealth](https://github.com/XiubaiZero/MakeUHealth)。

```powershell
git clone https://github.com/XiubaiZero/MakeUHealth.git
cd MakeUHealth
```

## 环境要求

- Node.js `^20.19.0 || >=22.12.0`，npm。
- JDK 17 或更高版本；后端编译目标为 Java 17。
- MySQL 8.0；数据库服务需独立启动。
- Maven 使用仓库中的 `mvnw.cmd`（Windows）或 `./mvnw`（macOS/Linux），无需额外安装 Maven。

## 本地启动

先在 MySQL 中创建空数据库：

```sql
CREATE DATABASE IF NOT EXISTS IPD CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

在 PowerShell 中启动后端：

```powershell
cd back_end
$env:SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/IPD?useSSL=false&serverTimezone=UTC&characterEncoding=utf8'
$env:SPRING_DATASOURCE_USERNAME='root'
$env:SPRING_DATASOURCE_PASSWORD='<本机数据库密码>'
# 如需 AI 回答，在本机设置 DEEPSEEK_API_KEY；不要提交密钥。
.\mvnw.cmd spring-boot:run
```

后端默认监听 `http://localhost:8081`。启动时执行 `schema.sql`、`assistant-schema.sql`、`assistant-memory-schema.sql`，再运行 `data.sql`，创建业务、聊天及记忆表，补齐受支持旧字段并初始化食物库。数据库用户需要相应建表和修改表权限。

另开终端启动前端：

```powershell
cd health-management-frontend
npm ci
Copy-Item .env.example .env.local
npm run dev
```

打开 Vite 输出的地址（默认 `http://localhost:5173`），注册账号后完善档案。

## 配置

| 变量 | 默认值／作用 |
| --- | --- |
| `VITE_API_BASE_URL` | `/api`；浏览器的 API 请求前缀 |
| `API_PROXY_TARGET` | `http://localhost:8081`；Vite 开发及预览代理目标，仅供服务端使用 |
| `SERVER_PORT` | `8081`；修改后同时调整代理目标 |
| `SPRING_DATASOURCE_URL` | 本地 MySQL `IPD` 库连接地址 |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | 数据库账号与密码；账号默认 `root`，密码通过本机环境变量设置 |
| `AUTH_JWT_SECRET` / `AUTH_JWT_EXPIRATION_SECONDS` | JWT 签名密钥与有效期，默认有效期 604800 秒 |
| `DEEPSEEK_API_KEY` | 后端调用 DeepSeek 的密钥 |
| `DEEPSEEK_MODEL` | `deepseek-flash`；v0.2.2 使用非思考模式 |
| `VITE_ASSISTANT_MODE` | `api`；其他值使用本地模板回答 |
| `VITE_ASSISTANT_API_URL` | `/assistant/chat`，相对于 API 请求前缀 |

前端 `.env.local` 修改后重启 Vite。生产静态部署应将 `/api` 反向代理至后端；Vite 代理不会写入 `dist`。如果使用独立 API 域名，构建时设置 `VITE_API_BASE_URL`，并在后端配置实际前端域名的 CORS。

新版聊天页面使用 `/api/assistant/conversations` 保存与读取记录，`VITE_ASSISTANT_API_URL` 仅控制保留的旧无状态入口。`VITE_ASSISTANT_MODE=local` 在新版页面仍需后端保存，只跳过付费模型调用。

## 聊天记录与跨设备

v0.2.2 增加当前会话多轮上下文、增量摘要和需要用户确认的长期记忆。聊天页的「管理记忆」可主动提取候选项；「设置 → 长期记忆」可管理确认后的资料。会话记忆和账号长期记忆可以分别关闭。详见 [使用与配置](docs/AI_MEMORY.md) 和 [真实参数对照结果](docs/AI_MEMORY_EVALUATION.md)。

- 会话与消息属于登录账号，保存于 MySQL；同一账号、同一后端及同一数据库的设备共享记录。
- 支持新建、重命名、切换、删除会话；默认读取最近 80 条消息，可加载更早记录，数据库没有 80 条截断。
- 进入、切换、返回前台和手动刷新时同步；当前页面等待回答期间短暂轮询。
- 后端先保存问题，异步生成并保存回答。离开页面不取消任务；重启或超时中断后可手动重试。编辑历史问题需确认删除原回答和后续消息，其他设备已更新时提示刷新。
- 旧浏览器记录可以主动导入独立会话，保留浏览器备份。已被旧保存限制淘汰的记录无法恢复。
- 服务不可用时保留当前已加载记录和草稿、暂停发送；聊天记录不提供离线缓存。草稿、当前选择及未确认发送元数据保存在当前标签页的账号隔离 sessionStorage，刷新后恢复。

局域网验证：电脑正常运行 MySQL、后端和前端，以 `npm run dev -- --host 0.0.0.0` 启动前端，让同一网络中的手机访问 `http://<电脑局域网IP>:5173`。手机与电脑都通过前端 `/api` 代理连接电脑后端；代理保留浏览器 Host。不要在手机上用 localhost 指代电脑。公网部署准备另见后续部署文档，本轮没有上线。

## 数据库脚本

- **正常初始化／增量升级：** `back_end/src/main/resources/schema.sql` → `data.sql`。也可由后端启动自动执行。这两个脚本支持重复执行。
- **旧结构升级范围：** 当前整合版表结构，包括补充食物库六项营养字段、餐次字段和索引、健康记录档案快照，以及每日餐次目标等表。已有营养值和食物记录不会被种子数据覆盖；新加营养字段允许为空。
- **历史演示数据：** 根目录 `integrated_ipd.sql` 含删表操作，只可导入一次性演示空库。导入后仍需运行当前初始化脚本。不要用它升级已有业务库。
- `back_end/ipd.sql`、`back_end/docs/IPD_FOOD_FIX.sql` 是旧版本材料，不作为当前初始化入口。更早且结构不同的数据库需先核对结构。

## 提醒行为

提醒属于登录账号，不要求先创建健康档案。前端登录后每 15 秒读取一次提醒，在网页内弹窗。已移除未接入前端的 WebSocket 和后端定时扫描。

- 使用浏览器本地日历时间计算日／周／月重复；月末按目标月份最后一天处理，之后恢复原始日期。例如 1 月 31 日 → 2 月 28/29 日 → 3 月 31 日。
- 重新打开网页时仅补提醒最近一次到期事项，不逐条补发所有历史重复事项。
- 确认记录按账号保存在当前浏览器，刷新后保留，并同步同源标签页。一次性提醒确认成功后会在后端禁用；失败时保留重试入口。
- 关闭网页或退出登录后不提供系统后台推送；重复提醒的确认状态不跨浏览器同步。浏览器后台计时器节流可能延迟弹窗。

## 语言与设置

侧栏底部的“设置 / Settings”小按钮打开设置弹窗，包含语言切换和退出登录；退出登录需要在弹窗中确认。登录、注册及档案引导页面的右上角也提供语言设置入口。

- 首次访问根据浏览器语言选择 English 或简体中文；手动选择后保存在当前设备，刷新及退出登录后保留，同源标签页同步更新。
- 导航、表单、按钮、提示、日期选择器、日期显示和图表标签即时更新，不重载页面。
- 用户录入的食物名称、备注及已有聊天内容保留原文；助手的新请求携带 `language: en | zh-CN`，服务端按选择的语言回答，本地兜底也支持双语。
- 文案维护入口为 `health-management-frontend/src/i18n/`。英文原文作为字典键，找不到翻译时保留原文。

移动端保留左侧图标与文字导航，手机宽度下缩窄侧栏与内容留白，健康总览的四张档案卡片排列为两列。档案和健康记录在常见手机宽度并排，360px 及以下屏幕纵向显示；短屏及横屏可以单独滚动导航，设置入口保持在底部。桌面仍使用四列档案卡片。

## 构建与测试

```powershell
cd health-management-frontend
npm test
npm run build
```

前端测试覆盖提醒日期、账号确认隔离、助手真实模块组合、资料组装、语言切换和 API 失败兜底等逻辑；构建包含 TypeScript 检查。

后端 AI 回归测试使用本地模拟 HTTP 服务，直接执行 `back_end` 中的 `.\mvnw.cmd test` 或 `.\mvnw.cmd package`，不需要数据库或 DeepSeek 密钥，不调用付费模型。

后端集成测试必须显式指定**独立测试库**，否则跳过，避免误连正常开发数据库：

```powershell
cd back_end
$env:HMS_TEST_DATABASE_URL='jdbc:mysql://127.0.0.1:3306/hms_test?useSSL=false&serverTimezone=UTC&characterEncoding=utf8'
$env:HMS_TEST_DATABASE_USER='root'
$env:HMS_TEST_DATABASE_PASSWORD='<测试库密码>'
.\mvnw.cmd test
.\mvnw.cmd package -DskipTests
```

先创建空的 `hms_test`。测试会写入测试账号和业务数据；迁移测试另建随机命名的 `hms_migration_*` 库并在结束后删除，因此测试账号需有创建和删除测试数据库权限。测试覆盖真实 HTTP 注册登录、健康记录与快照、账号隔离、饮食统计、热量目标、健身进度、提醒 CRUD、助手缺少配置，以及旧库升级和重复初始化。不会调用付费 AI 接口。

## 目录与接口说明

- `health-management-frontend/`：当前前端代码。
- `back_end/`：当前后端代码。
- `_merge_tmp/`：本机历史合并材料，不参与当前构建，不上传至 GitHub。
- [后端结构](back_end/docs/PROJECT_STRUCTURE.md)
- [接口清单](back_end/docs/API_CHECKLIST.md)
- [AI 完整链路、扩展边界与版本迭代](docs/AI_CHAIN.md)
- [聊天记录、生成任务与接口](docs/CHAT_PERSISTENCE.md)
- [Docker 部署、备份与恢复准备](docs/DEPLOYMENT.md)
- [版本更新记录](CHANGELOG.md)
