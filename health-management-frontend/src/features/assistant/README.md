# 健康助手模块

`src/api/assistant.ts` 保留 `requestAssistantReply(question, context): Promise<string>` 和原类型导出，负责能力介绍提前返回、API/本地模式选择和失败兜底。

| 文件 | 职责 |
| --- | --- |
| types.ts | 健康上下文、目标快照和助手内部类型 |
| rules.ts | 原范围、能力判断与页面建议问题规则；保留三者判断差异 |
| localReplies.ts | 中英文能力介绍、参考回答、目标摘要和服务失败标记 |
| context.ts | 纯函数组装原健康资料、当前及全部目标快照，选择最近记录及统计七天饮食记录 |
| transport.ts | 原请求字段、五条附加要求和 60 秒请求超时 |

`SmartAssistantView.vue` 管理 API 数据加载、Vue 状态、账号隔离的历史记录和交互，调用 `context.ts` 及 `rules.ts` 的纯函数。资料输入与当前时间通过参数传入，不在模块内缓存用户数据。语言在每次调用时读取，不在模块加载时缓存。所有请求仍使用既有 apiClient，不读取模型密钥，不上传聊天历史。

后续修改问题识别与建议规则在 `rules.ts` 接入；本地文案在 `localReplies.ts`；健康资料字段在 `types.ts` 和 `context.ts`；请求与附加要求在 `transport.ts`。模型侧提示词与语义分类边界见[完整链路说明](../../../../docs/AI_CHAIN.md)。

测试通过 Node 与现有 TypeScript 转译器加载真实模块依赖图，仅替换 API 客户端与语言状态边界；每个测试有独立模块缓存。
