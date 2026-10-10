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

## v0.2.4 提示词资源

`AssistantPromptResources` 使用 `ClassPathResource.getInputStream()`、UTF-8 和 try-with-resources 加载文案，各组件初始化时缓存结果。资源缺失、空白或含 UTF-8 BOM 时初始化失败，错误保留路径及原因；请求失败兜底不掩盖资源错误。

资源位于 `src/main/resources/prompts/`：

| 文件 | 使用位置 |
| --- | --- |
| answer-v1.txt | AssistantPromptBuilder 的问答 system prompt |
| memory-extraction-v1.txt | AssistantMemoryExtractor 的提取 system prompt |
| summary-v1.txt | AssistantSummaryProcessor 的摘要 system prompt |
| english-rewrite-v1.txt | AssistantPromptBuilder 的英文改写 |
| memory-instructions-v1.txt | AssistantService 的预校验、正常生成及摘要省略后的 system prompt |
| memory-facts-label-v1.txt | AssistantService 的已确认长期记忆段落标题 |
| conversation-summary-label-v1.txt | AssistantService 的历史摘要段落标题 |

前三个文件必须各含一个 `{{languageInstruction}}`，初始化时检查，运行时替换为原有 Java 语言短句；用户问题、健康资料、历史和记忆不执行模板替换。现有 `intent-v1.txt` 及分类器版本元数据保持原样。

文件保存 UTF-8 无 BOM 正文，加载器只移除末尾连续 CR/LF，不做 trim/strip，不改变正文内部空白。记忆规则不含前导连接空格，Java 显式补一个空格；两个标题不含结构换行，Java 按 `"\n\n" + label + "\n"` 拼接。短语言规则、数据标题、输入预算和校验代码仍由 Java 管理。

文件随 JAR 发布，修改后重新打包并重启。本版没有动态 registry、热更新或黄金请求文本测试，现有回归测试和五项加载检查不作为完整请求逐字一致性的证明。
