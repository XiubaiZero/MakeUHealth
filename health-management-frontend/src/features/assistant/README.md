# 健康助手模块

`src/api/assistant.ts` 保留 `requestAssistantReply(question, context): Promise<string>` 和原类型导出，负责能力介绍提前返回、API/本地模式选择和失败兜底。

| 文件 | 职责 |
| --- | --- |
| types.ts | 健康上下文、目标快照和助手内部类型 |
| rules.ts | 原范围、能力判断与页面建议问题规则；保留三者判断差异 |
| localReplies.ts | 中英文能力介绍、参考回答、目标摘要和服务失败标记 |
| context.ts | 纯函数组装原健康资料、当前及全部目标快照，选择最近记录及统计七天饮食记录 |
| transport.ts | 原请求字段、五条附加要求和 60 秒请求超时 |

`SmartAssistantView.vue` 管理健康资料加载和页面交互，调用 `context.ts` 组装上下文。新版记录通过 `conversations/api.ts`、`session.ts` 管理云端会话、分页、任务状态、草稿、请求确认和账号/页面切换隔离；`legacy.ts` 只读取并导入旧存储键，不再保存新聊天历史。所有请求使用既有 apiClient，不读取模型密钥；已有聊天内容不自动进入模型请求。

进入页面、切换会话、返回前台及手动刷新时更新。当前页面等待生成时每两秒查询，隐藏或卸载时停止。草稿及未确认请求使用按账号隔离的 sessionStorage，服务器不可用时不创建离线消息。未确认发送沿用原 UUID，刷新后先恢复原会话并查询任务。

`localReplies.ts` 保留旧 API 的兼容逻辑；新版完整回答归属后端 `JavaAssistantEngine`，迁移文案通过 56 组浏览器输出基准验证。健康资料字段在 `types.ts` 和 `context.ts`，请求与附加要求在 `transport.ts`。模型侧提示词、语义分类及未来 Python 引擎边界见[完整链路说明](../../../../docs/AI_CHAIN.md)。

测试通过 Node 与现有 TypeScript 转译器加载真实模块依赖图，仅替换 API 客户端与语言状态边界；每个测试有独立模块缓存。
