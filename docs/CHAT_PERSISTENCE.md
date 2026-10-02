# 聊天记录与生成任务

版本统一采用 `v0.2.x`，直到项目所有者另行指定。已有发布历史不改写。

## 后端

`AssistantConversationService` 管理会话、账号隔离、版本检查和短事务；`AssistantEngine` 管理完整回答，可替换为独立 Python 服务。现有 `AssistantService.ask()` 与 `/api/assistant/chat` 继续兼容。

发送先提交问题和任务，返回 202；有界执行器（2 个工作线程、8 个排队位置）在事务外生成，再提交回答。单个会话仅允许一轮生成。请求 UUID 在会话内唯一，重试不会再次生成；已删除或被编辑替代的请求返回 410。

新接口前缀 `/api/assistant/conversations`：

| 方法与路径 | 行为 |
| --- | --- |
| GET / | 列表，offset=0、limit=20，最多 100 |
| POST / | 创建空会话 |
| GET /{id}/messages | 最近 80 条，before 为消息顺序游标；含会话版本、当前任务及失败任务 |
| PATCH /{id} | expectedRevision、title，标题 1–100 字符 |
| DELETE /{id}?expectedRevision=... | 删除会话及其消息、任务 |
| POST /{id}/turns | requestId、expectedRevision、message、language、context、constraints、mode；可选 editMessageId |
| GET /{id}/turns/{requestId} | 查询生成状态 |
| POST /{id}/messages/delete | expectedRevision、messageIds |
| POST /import | messages（role、content、suggestionPrompts）、language |

所有操作基于 JWT 账号归属，其他账号访问返回 404。修改时锁定会话行，版本不匹配返回 409。模型调用期间不占用事务。删除会话后不保存迟到结果；编辑问题截断其后的消息，同时保留旧请求墓碑以防重试恢复内容。

生成任务有 queued/running/completed/failed/deleted 状态。服务重启后未完成任务标记失败；运行超过 180 秒的任务停止接受结果。不会自动重新调用付费模型。完成或失败后清除任务上下文；服务存活且数据库可用时，离开页面不影响生成。数据库故障期间不能承诺保存结果，恢复后会释放超时任务。当前执行与启动恢复策略面向单个后端实例。

能力介绍与本地参考回复由 `JavaAssistantEngine` 生成，模型失败保留双语兜底标记。历史不会自动传给模型。后续 Python 引擎只接收授权上下文和返回回答，不直接修改聊天表。

导入使用服务器计算的 SHA-256 指纹，账号内去重；新建独立导入会话，不覆盖云端历史。旧消息没有可信时间，createdAt 为空，保持原顺序。不同内容的旧记录分别导入。
