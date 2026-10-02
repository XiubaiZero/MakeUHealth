# AI 链路与扩展边界

`AssistantService.ask()` 是协调入口。当前依次检查问题、分类意图、提前返回固定回复或缺少配置提示、格式化资料、生成提示词、调用模型、清理回答和必要时进行一次英文改写。

第一阶段拆出的组件位于 `com.example.ipd_sp_back_end.assistant`：

| 组件 | 职责与扩展点 |
| --- | --- |
| AssistantIntentClassifier / RuleBasedAssistantIntentClassifier | 分类接口与当前规则实现；后续语义分类替换实现，保留三类结果与提前返回顺序 |
| AssistantReplyCatalog | 中英文能力、范围外和缺少配置的固定回复 |
| AssistantContextFormatter | 保持资料 Map 的顺序、值格式与附加要求格式 |
| AssistantPrompt / AssistantPromptBuilder | 独立生成问答和英文改写提示词；后续提示词模板升级在此接入 |

意图判断与提示词生成独立维护。关键词、正则和提示词在本轮原样迁移。HTTP 调用和回答处理暂留在服务中，下一阶段提取；本阶段仍可独立构建。

回归测试通过本地模拟 HTTP 服务验证协议、语言、错误和并发请求，不访问付费模型。真实数据库测试需要独立的 `HMS_TEST_DATABASE_URL` 配置。
