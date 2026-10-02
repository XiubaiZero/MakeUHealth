# AI 记忆 — v0.2.2

本版本沿用 Java 业务、鉴权与 MySQL，增加当前会话上下文、增量摘要及经用户确认的长期资料。三个阶段分别审阅合并，只在最终功能里程碑发布 v0.2.2；不增加 Python 服务、知识库、向量检索或跨会话原文搜索。真实参数结果见 [AI_MEMORY_EVALUATION.md](AI_MEMORY_EVALUATION.md)。

## 如何使用

1. 后端连接原数据库并启动，会自动新增记忆表；保留原有会话和消息。
2. 设置 DEEPSEEK_API_KEY 后，API 模式默认使用 deepseek-flash 非思考模式。界面语言决定新回答语言。
3. 当前会话默认开启「会话记忆」，新问题使用较近完整问答及较早摘要。页面的 80 条是读取页大小，与模型历史 12 轮无关；后端取历史，浏览器不能提交别人的历史。
4. 聊天页「管理记忆 → 从本会话提取」一次点击最多一次辅助调用，只处理最近 12 条用户消息，最多产生 5 项待确认资料。展开来源、编辑后点击确认；候选不会直接进入模型。
5. 「设置 → 长期记忆」与聊天页使用同一账号数据，可手动添加、编辑、删除、清空及关闭。关闭账号长期记忆保留资料并暂停注入/提取，当前会话上下文仍可用。关闭会话记忆则停用该会话的历史、摘要和长期资料，保留聊天记录。

确认记忆默认最多 20 条，每条最多 200 个 Unicode 字符，候选最多 50 条。满额时先删除或明确选择被替换条目，不自动覆盖。来源消息被编辑/删除时，对应候选和已确认资料一起删除；编辑后续消息保留无关的早期资料。手动添加的条目独立于聊天来源。清空不删除聊天。

设置及条目有版本号，另一设备修改后旧操作返回 409，刷新后再操作。提取失败不自动重复付费调用；重启或超时将任务标记失败。刷新页面后已完成候选仍可查看；未完成任务可稍后刷新管理界面，避免反复提取。

## 后端链路及一致性

会话生成短事务领取任务、读取不可变 MemorySnapshot 并提交。摘要、主问答、至多一次英文改写均在事务外调用，完成后短事务验证账号版本、会话开关版本及内容版本，再一起保存回答、usage 和摘要。进行中变更记忆会使迟到答案失败，不保存旧资料结果。

近期历史仅使用成功模型问答及有效相邻导入问答；失败、本地、固定、兜底回答不当作模型历史。每轮最多一次增量摘要：每批最多 20 个较早完整轮次，按最多 8,192 个保守输入单位选取；输出最多 4,096 UTF-8 字节，记录覆盖的消息序号。超长旧消息及未覆盖部分不声称已记住。摘要失败保留旧摘要和近期对话并显示提示。输入预算减掉最旧完整问答，不截断当前问题；必要资料超限在问题保存前返回 400，前端保留草稿。旧摘要过大时可以省去并标明。

长期提取用独立有界执行器。请求 UUID 去重，单账号同时最多一个提取任务；任务先持久化、事务外调用，完成时复核来源内容哈希和版本。只输入用户原文；JSON、类别、长度、消息 ID 及精确引用必须有效。候选入库不改变已确认资料版本；确认、编辑、删除及清空提高账号版本，阻止旧任务复活。HTTP 调用不占用数据库事务。

扩展位置：AssistantMemoryService 负责快照、摘要和来源失效；AssistantPersonalMemoryService 负责资料和容量；AssistantMemoryExtractor 负责提示词与结构验证；AssistantMemoryExtractionService 负责任务；AssistantSummaryProcessor 负责摘要；AssistantService 负责生成协调；AssistantEngine 保留未来 Python 整轮服务边界。意图规则与记忆提示词独立维护。

## 配置

Spring 支持属性和大写下划线环境变量。默认值经过本轮比较，但不是普遍最优。输入单位是 UTF-8 字节加消息开销的保守估计，实际 token 以 API usage 为准。

| 属性 | 环境变量 | 默认值 |
| --- | --- | ---: |
| assistant.deepseek.model | DEEPSEEK_MODEL | deepseek-flash |
| assistant.deepseek.temperature | ASSISTANT_DEEPSEEK_TEMPERATURE | 0.4 |
| assistant.deepseek.max-tokens | ASSISTANT_DEEPSEEK_MAXTOKENS | 4096 |
| assistant.memory.recent-rounds | ASSISTANT_MEMORY_RECENTROUNDS | 12 |
| assistant.memory.input-budget | ASSISTANT_MEMORY_INPUTBUDGET | 32768 |
| assistant.memory.context-window | ASSISTANT_MEMORY_CONTEXTWINDOW | 1048576 |
| assistant.memory.summary-max-tokens | ASSISTANT_MEMORY_SUMMARYMAXTOKENS | 2048 |
| assistant.memory.extraction-max-tokens | ASSISTANT_MEMORY_EXTRACTIONMAXTOKENS | 2048 |
| assistant.memory.auxiliary-timeout-seconds | ASSISTANT_MEMORY_AUXILIARYTIMEOUTSECONDS | 20 |
| assistant.memory.summary-temperature | ASSISTANT_MEMORY_SUMMARYTEMPERATURE | 0 |
| assistant.memory.extraction-temperature | ASSISTANT_MEMORY_EXTRACTIONTEMPERATURE | 0 |
| assistant.memory.capacity | ASSISTANT_MEMORY_CAPACITY | 20 |

连接超时 20 秒，主调用每次 60 秒；辅助可配置 1–40 秒。页面使用任务短轮询。原 `/api/assistant/chat` 无状态入口及旧本地 API 保持兼容；密钥仅后端读取。

新增接口均在 `/api/assistant` 下：GET/PATCH/DELETE `/memory` 管理账号状态/清空；POST `/memory/items`、PATCH/DELETE `/memory/items/{id}` 管理条目；GET/PATCH `/conversations/{id}/memory` 管理会话开关；POST `/conversations/{id}/memory/extractions` 发起任务，GET 其后 `/{requestId}` 读取状态。要求当前账号鉴权；400 为输入/预算错误，404 为不可见资源，409 为版本/容量/并发冲突，503 为配置或存储不可用。

## 验证与部署

JUnit 包含 H2 确定性测试，真实 MySQL 仅在 HMS_TEST_DATABASE_URL 为独立测试库时执行，强制禁用付费模型。Node 测试使用真实 TypeScript 模块组合及传输替换。浏览器另检查来源、跨设备与手机布局。实测驱动只有手动执行才访问付费模型。

Git Release 和 Docker 默认标签统一为 v0.2.2；已有 deploy/.env 需更新 HMS_IMAGE_TAG。新增表保留原数据升级；回退前备份，旧版本忽略新增表。没有上传镜像或部署公网。本机 Docker Desktop 无法启动，容器验证状态见 [部署文档](DEPLOYMENT.md)。
