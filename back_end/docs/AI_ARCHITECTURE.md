# AI 链路与扩展边界

`AssistantService.ask()` 是协调入口。当前依次检查问题、分类意图、提前返回固定回复或缺少配置提示、格式化资料、生成提示词、调用模型、清理回答和必要时进行一次英文改写。

组件位于 `com.example.ipd_sp_back_end.assistant`：

| 组件 | 职责与扩展点 |
| --- | --- |
| AssistantIntentClassifier / RuleBasedAssistantIntentClassifier | 分类接口与当前规则实现；后续语义分类替换实现，保留三类结果与提前返回顺序 |
| AssistantReplyCatalog | 中英文能力、范围外和缺少配置的固定回复 |
| AssistantContextFormatter | 保持资料 Map 的顺序、值格式与附加要求格式 |
| AssistantPrompt / AssistantPromptBuilder | 独立生成问答和英文改写提示词；后续提示词模板升级在此接入 |
| ChatModelClient / DeepSeekChatClient | 配置检查接口、同步 HTTP 调用、鉴权及 JSON 请求与响应；后续模型提供方替换在此接入 |
| DeepSeekProperties / AssistantConfiguration | 绑定原有 `assistant.deepseek.*` 配置，密钥仍来自后端 `DEEPSEEK_API_KEY` |
| AssistantResponseProcessor | 回答首尾空白清理及英文改写条件；服务协调最多一次额外调用 |

意图判断与提示词生成独立维护。关键词、正则和提示词在本轮原样迁移。服务通过构造器注入组件，只负责顺序、提前返回和异常包装。组件不保存请求级共享可变状态。

`ChatModelClient` 提供 `isConfigured()` 与 `complete(AssistantPrompt)`。客户端保持连接超时 20 秒、单次请求超时 60 秒、温度 0.4；中文一次调用，英文回答含汉字时再改写一次，改写失败沿用 HTTP 500。现有 `/api/assistant/chat` DTO、错误正文和 HTTP 400/500 均保持不变。

回归测试通过本地模拟 HTTP 服务验证协议、语言、错误和并发请求，不访问付费模型。真实数据库测试需要独立的 `HMS_TEST_DATABASE_URL` 配置。
