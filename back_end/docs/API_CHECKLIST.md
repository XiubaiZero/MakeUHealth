# 后端接口清单

本地后端：`http://localhost:8081`，统一前缀 `/api`，请求及响应正文为 JSON。前端默认经 Vite 代理请求 `/api`。

## 认证与数据归属

只有 `/api/auth/register` 与 `/api/auth/login` 无需登录。其他业务请求必须携带 `Authorization: Bearer <token>`。登录返回 `accountId`、`account`、`accountType`、`token`、`message`。

账号支持邮箱或手机号，密码至少 6 位。注册后需要登录获取 token。

`userId` 表示健康档案 ID，不等于账号 ID。服务端从 JWT 解析账号，再通过 `account_user_binding` 获取当前档案；历史接口路径或请求体中的用户 ID 不会改变数据归属。按资源 ID 访问健康记录、健身进度、提醒时仍会检查归属。提醒按账号保存，允许未创建档案时使用。

## 注册登录与档案

| 方法 | 路径（省略 /api） | 行为／请求 |
| --- | --- | --- |
| POST | /auth/register | `{account, password}`，创建账号 |
| POST | /auth/login | `{account, password}`，获取 JWT |
| GET | /users | 当前账号的档案数组（0 或 1 项） |
| POST | /users | `{age, gender, height, weight}`，创建或更新当前档案 |
| GET | /users/{id} | 返回当前档案，id 为兼容参数 |
| PUT | /users/{id} | 更新当前档案，正文同 POST |
| DELETE | /users/{id} | 删除当前档案，成功返回 204 |

`gender` 为 `male` 或 `female`，身高单位 cm，体重单位 kg。

## 健康记录

| 方法 | 路径 | 行为 |
| --- | --- | --- |
| POST | /health-records/user/{userId} | 新增当前档案的记录 |
| GET | /health-records/user/{userId} | 当前档案的记录列表 |
| GET | /health-records/{id} | 当前档案的一条记录；不可访问其他用户记录 |
| DELETE | /health-records/{id} | 删除当前档案对应记录，成功返回 204 |

录入正文示例：

```json
{"systolic":120,"diastolic":80,"fbg":5.2,"heartRate":70,"oxyhemoglobin":98,"recordedAt":"2026-10-01T10:00:00"}
```

保存时后端附加 `ageSnapshot`、`genderSnapshot`、`heightSnapshot`、`weightSnapshot`，后续更新档案不会改变已有快照。

## 食物库、摄入与每日目标

| 方法 | 路径 | 行为 |
| --- | --- | --- |
| GET | /food-library | 共享食物库 |
| GET | /food-library/search?name=Rice | 按名称精确查询 |
| GET | /food-library/by-name?foodName=Rice | 精确查询的兼容接口 |
| GET | /food-library/search/keyword?keyword=Rice | 按关键词查询 |
| POST | /food-intake | 新增当前档案的摄入记录 |
| GET | /food-intake/user/{userId} | 当前档案的摄入记录 |
| DELETE | /food-intake/{id} | 删除当前档案对应记录 |
| GET | /food-intake/stats?type=day | 当前档案的营养统计，userId 查询参数可省略 |
| GET | /food-intake/stats/{userId}/{type} | 统计的兼容路径 |
| GET | /food-intake/stats/{userId}/{type}/range?start=…&end=… | 指定时间段，start/end 为 ISO 本地日期时间 |
| GET | /food-intake/by-meal?date=2026-10-01 | 按餐次分组，返回实际热量及条目 |
| GET | /meal-targets?date=2026-10-01 | 当日目标，无目标时可为空 |
| POST | /meal-targets | 按当前档案及日期创建或更新热量目标 |

摄入记录正文：

```json
{"foodName":"Rice","amount":100,"unit":"g","calories":116,"nutrients":"Carbohydrate","intakeTime":"2026-10-01T12:00:00","mealType":"lunch"}
```

`mealType` 使用 `breakfast`、`lunch`、`dinner` 或 `snack`；未提供时后端默认 `snack`。`type` 使用 `day`、`week`、`month`。

每日目标正文：

```json
{"targetDate":"2026-10-01","breakfastTarget":400,"lunchTarget":600,"dinnerTarget":500,"snackTarget":100}
```

食物库含每 100g 的 `protein`、`carbs`、`fat`、`fiber`、`sodium`、`sugar`。当前统计接口汇总热量和营养标签频次，尚未计算这些详细营养素的实际摄入总量，相关统计字段可为空。

## 健身目标

| 方法 | 路径 | 行为 |
| --- | --- | --- |
| GET | /fitness-goals | 当前档案的全部目标 |
| GET | /fitness-goals/active | 最新活动目标 |
| GET | /fitness-goals/active/{goalType} | 指定类型的活动目标 |
| POST | /fitness-goals | 创建目标，同类型旧活动目标归档 |
| POST | /fitness-goals/calculate | 预计算总周数及每周变化 |
| POST | /fitness-goals/goal/{goalId}/progress | 创建本周进度 |
| PUT | /fitness-goals/goal/{goalId}/progress | 更新本周进度 |
| GET | /fitness-goals/goal/{goalId}/progress | 目标的周进度 |
| GET | /fitness-goals/progress | 当前档案全部周进度 |
| GET | /fitness-goals/progress/{goalType} | 指定类型最新目标的进度 |
| GET | /fitness-goals/dashboard | 当前活动目标概况 |
| GET | /fitness-goals/dashboard/{goalType} | 指定类型最新目标概况（可含已完成目标） |

`goalType` 为 `weight_loss`、`fat_loss`、`muscle_gain`。创建正文为 `{goalType,currentValue,targetValue,targetDate}`，预计算无需 goalType，targetDate 使用未来日期 `YYYY-MM-DD`。周进度正文为 `{currentValue}`。

## 提醒

| 方法 | 路径 | 行为 |
| --- | --- | --- |
| GET / POST | /reminders | 列出当前账号提醒／创建提醒 |
| PUT / DELETE | /reminders/{id} | 更新／删除当前账号的一条提醒 |
| DELETE | /reminders | 删除当前账号的全部提醒 |
| GET / DELETE | /reminders/user/{userId} | 兼容入口，仍以当前账号为范围 |

正文示例：

```json
{"reminderType":"exercise","reminderTime":"2026-10-01T18:00:00","repeatPattern":"daily","note":"Walk for 20 minutes","enabled":true}
```

前端类型：`medication`、`meal`、`exercise`、`checkup`、`sleep`、`custom`。重复周期：`none`、`daily`、`weekly`、`monthly`。由前端每 15 秒轮询并判断到期，没有 WebSocket 接口。详细重复与确认规则见根目录 README。

## 智能助手

`POST /assistant/chat`，正文 `{message,language,context,constraints}`，返回 `{answer}`。language 支持 `en` 和 `zh-CN`，缺省使用英文。context 为前端整理的个人资料、健康指标和目标概况；constraints 为字符串数组。

后端使用 `DEEPSEEK_API_KEY` 调用 DeepSeek，限定健康、饮食和健身主题，并按 language 回答。缺少密钥时返回对应语言的配置提示。前端在接口失败时使用对应语言且明确标记的本地模板回答。

## 错误与验证

错误响应目前未完全统一：认证、健身、助手接口可返回 `error` 或 `message`；其他接口可能使用 Spring 标准错误正文。前端请求封装兼容这些字段。无有效身份通常返回 401，访问不属于自己的健康记录或提醒返回 404。

真实接口和数据库回归测试的运行方式见[根目录 README](../../README.md)。
