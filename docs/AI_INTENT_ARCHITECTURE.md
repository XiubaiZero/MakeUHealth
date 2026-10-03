# v0.2.3 意图路由与聊天偏好

## 后端链路

会话服务保存问题并取得授权的记忆快照，然后在事务外调用引擎。API 引擎只调用一次 AssistantService，服务只生成一次 AssistantIntentDecision，并将结果用于固定回复或主问答。分类和主问答分别使用 ChatCompletionOptions，不修改共享客户端配置。

AssistantIntentClassifier 保留字符串方法供规则、本地模式使用；结构化方法接收 AssistantIntentInput 和本轮期限。SemanticAssistantIntentClassifier 是默认注入实现，负责模式选择、受限上下文、严格 JSON 和失败降级；RuleBasedAssistantIntentClassifier 提供兼容规则。独立提示词保存在 prompts/intent-v1.txt；未来更换语义分类服务或修改分类提示词，不需调整记忆与存储服务。

分类失败采用 HEALTH 问答路径，但元数据 category=UNKNOWN、status=degraded，保留真实降级含义。固定回复即使使用过分类模型，也不计为主问答使用历史或长期记忆。API 主问答生成的正常回答继续作为后续历史轮次；本地和失败参考回答保留原标记。

原同步入口的整轮期限为 55 秒，分类、主问答和最多一次英文改写共享剩余时间。异步入口保留原单次超时和任务过期机制。摘要接口支持接收期限，不改变其原配置上限。HTTP 模型的异常仍为 500，空白或必要资料超预算为 400。

消息 memory 元数据可选增加 intent，以及 calls[].purpose=intent/summary/answer/rewrite。旧记录缺少这些字段仍可读取。生产降级日志仅记录原因枚举、历史数量和耗时，不记录问题、健康资料、完整提示词或密钥。

## 账号偏好接口

GET /api/assistant/preferences 返回 enterSendEnabled 和 revision；首次读取创建默认开启的记录。PATCH 接收 enterSendEnabled 和 expectedRevision，在短事务中校验版本，冲突返回 409。账号只来自认证身份。偏好表与记忆表独立，变更不影响生成中的记忆快照；账号删除时级联删除偏好。

前端设置弹窗和聊天页共享偏好状态，成功保存后才应用新值；首次读取失败使用默认开启，后续失败保持已确认值。状态使用账号范围和响应序号丢弃迟到请求。可见聊天页每 15 秒同步，设置打开和恢复可见时主动刷新。

composer.ts 提供键盘决策、空白检查和光标换行；页面负责组合输入状态、弹窗焦点、编辑确认和现有 cloud.send。关闭 Enter 发送后直接沿用浏览器旧输入行为。偏好不是离线配置，不在 localStorage 写入未确认的新值。

## 验证与回退

普通 Maven、Node 测试和 GitHub Actions 不调用付费模型。真实模型评测通过 evaluation/ai-intent/run.mjs 和 Java 驱动，费用账本位于忽略目录，发布证据使用虚构资料并单独提交。完整结果见 AI_INTENT_EVALUATION.md。

设置 ASSISTANT_INTENT_MODE=rules 并重启后端可回退路由。输入快捷键可在聊天页设置关闭；版本回退保留偏好数据表。Compose 默认标签 v0.2.3 只是本次版本配置，未上传镜像。
