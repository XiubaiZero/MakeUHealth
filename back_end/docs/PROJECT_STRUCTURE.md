# 后端项目结构

技术栈：Java 17、Spring Boot 4.0.0、Spring Security、JWT、MyBatis-Plus、MySQL 8。启动步骤见[根目录 README](../../README.md)。

| 目录 | 职责 |
| --- | --- |
| `config` | MyBatis-Plus 与 HTTP 安全配置 |
| `controller` | 注册登录、档案、健康记录、食物库、饮食记录、每日餐次目标、健身目标、提醒、助手的 REST 接口 |
| `service` | 账号归属、健康快照、饮食汇总、目标进度、提醒持久化及 DeepSeek 调用 |
| `security` | JWT 签发与校验、获取当前登录账号 |
| `entity` / `mapper` | 数据表映射及 MyBatis-Plus 数据访问 |
| `dto` | 登录、助手、营养统计等请求／响应对象 |
| `src/main/resources` | 应用配置、幂等 schema.sql 与 data.sql |
| `src/test/java` | 显式指定测试库后执行的 HTTP 与数据库集成测试 |

## 数据归属

`auth_user` 保存登录账号，`user` 保存健康档案，`account_user_binding` 将二者一对一关联。健康记录、饮食、每日目标、健身目标和周进度均关联档案 ID。提醒以 `account_id` 为归属依据，允许未完善档案的账号使用；提醒表的旧 `user_id` 字段保留兼容，但不作为鉴权依据。食物库由已登录用户共享读取。

业务请求以 JWT 中的账号为准，不信任客户端提供的用户 ID。同类健身目标新建时会归档之前的活动目标。健康记录保留录入时的年龄、性别、身高、体重快照。

## 提醒与助手

提醒通过 REST 保存和读取，由前端每 15 秒检查到期并弹窗；没有 WebSocket 服务或后端提醒调度器。提醒重复规则、补提醒及确认范围见根目录 README。

助手通过后端调用 DeepSeek；当前上下文由前端整理后传入，并非后端直接读取完整病历。缺少 API 密钥时返回明确提示；请求失败时前端可返回带兜底标识的本地模板回答。

## 数据库脚本

当前启动入口为 `src/main/resources/schema.sql` 和 `data.sql`。`ipd.sql`、`docs/IPD_FOOD_FIX.sql`、根目录 `integrated_ipd.sql` 均为历史材料，后者含删表语句，仅供一次性演示库使用。
