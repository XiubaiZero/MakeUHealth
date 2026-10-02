# 单机部署准备（v0.2.x）

本轮只交付部署文件与验证结果，没有购买资源、注册域名或发布公网服务。后续预算约每月 50–150 元，兼顾中国及海外用户；购买时再评估香港或附近地区的实际套餐与访问体验。先评估 2 核、2 GB 或更高内存的 Linux 主机，镜像构建可在资源更充足的机器完成。

验证环境：Docker Engine 29.3.1 / Compose 5.1.1，前后端镜像构建成功，MySQL 8.4、Java 后端与 Nginx 均健康。通过真实 Nginx 验证注册、档案、食物库和聊天保存；模拟 HTTPS 代理头下 POST 成功；实际备份、清空测试消息并恢复后，账号和聊天记录完整保留。没有访问付费模型或真实业务数据库。

Windows 中文工作区下 Docker BuildKit 可能出现非 ASCII 请求头错误；此次通过将必要源码复制到英文临时路径完成验证。遇到同类错误时使用英文路径检出，不需要修改项目内容。目标 Linux 主机也建议使用 /opt/MakeUHealth 等英文路径。

## 本地或测试主机运行

安装并启动 Docker Engine/Compose。从项目根目录执行：

```bash
cp deploy/.env.example deploy/.env
# 编辑 deploy/.env，填写两个不同的数据库密码及 JWT 密钥。
# 可用 openssl rand -base64 48 生成 JWT 密钥；密钥文件只留在主机。
chmod 600 deploy/.env
docker compose --env-file deploy/.env config -q
docker compose --env-file deploy/.env up -d --build
docker compose --env-file deploy/.env ps
```

默认只允许本机访问 `http://127.0.0.1:8080`。需要局域网测试时，把 HMS_BIND_ADDRESS 改为电脑的局域网 IP，再允许该端口在受信任局域网中访问。两个设备必须使用同一地址、同一账号。

只有前端开放端口；后端和 MySQL 通过 Compose 内部网络通信。MySQL 数据存在命名卷中，正常重建或停止容器不删除数据。**不要用 `docker compose down -v` 执行升级或回退。**

前端 Nginx 保存浏览器 Host，`/api` 转发到 Java；Java 识别代理头，浏览器不直接调用模型。DEEPSEEK_API_KEY 只出现在后端运行环境。前端构建环境文件通过 .dockerignore 排除，使用默认同源 `/api` 与 API 模式。

数据库健康检查验证应用账号能够执行 SELECT 1；后端健康检查验证监听端口；后端启动失败时前端不会提前启动。数据库后续故障由聊天接口返回存储错误，端口健康检查不代表持续数据库可用。

## 备份与恢复

```bash
bash deploy/backup.sh
# 在独立测试环境确认备份可恢复后，再用于生产恢复。
bash deploy/restore.sh --replace-database /absolute/path/IPD-YYYYMMDDTHHMMSS-PID.sql.gz
```

备份使用一致性事务导出，生成 gzip 文件，失败时删除临时文件。默认目录 deploy/backups 已忽略 Git；定期把备份复制到独立存储，服务器上的单份文件不能应对磁盘损坏。

恢复会先备份当前数据库，停止后端，替换备份中的表内容，再启动服务。如果导入失败，后端保持停止，先修复或恢复上一份完整备份，不自动启动半恢复的数据。每次升级前备份；每月至少在独立测试库验证一次恢复。可用 HMS_ENV_FILE/HMS_BACKUP_DIR 指定其他环境文件和备份目录。

Linux 计划任务示例：

```cron
0 3 * * * /bin/bash /opt/MakeUHealth/deploy/backup.sh >> /opt/MakeUHealth/deploy/backups/backup.log 2>&1
```

先创建备份目录；按实际路径配置，并自行确定备份保留周期。脚本不自动删除已有备份。

## 升级与回退

每次发布使用新的 v0.2.x 标签与 HMS_IMAGE_TAG，保留旧镜像。先备份，在干净检出中切到目标标签，更新环境文件中的镜像标签并重建：

```bash
bash deploy/backup.sh
git switch --detach v0.2.2
docker compose --env-file deploy/.env up -d --build
docker compose --env-file deploy/.env ps
```

后续版本回退时，选择之前已经验证、包含部署文件的版本及对应镜像标签，执行 up -d --no-build。数据库仍保留；若未来版本有不兼容数据库变更，先在停机窗口恢复升级前备份。本轮新增表不删除原业务数据。v0.2.0 未包含 Docker 文件，首次容器部署从 v0.2.1 开始。

v0.2.2 新增记忆设置、摘要、候选项、来源及提取任务表，启动时自动建表，不删除原聊天。升级前备份；回退 v0.2.1 时新增表可保留，旧版本会忽略它们。已有 deploy/.env 的 HMS_IMAGE_TAG 需手动改为 v0.2.2；本轮没有上传镜像或部署公网。当前机器的 Docker Desktop 启动因 dockerInference 本地监听文件错误失败，v0.2.2 容器构建/启动尚未验证；Java、Vue 构建和独立 MySQL 的原生运行已验证，不沿用 v0.2.1 的容器测试结果作为本版证据。

## 域名与 HTTPS 上线准备

资源准备好后：确定服务器地区与域名，把 DNS 指向服务器，配置主机 Nginx 和 HTTPS 证书，再进行一次明确的上线操作。本轮不执行这些步骤。

容器继续绑定 127.0.0.1:8080，由主机 Nginx 接收公网 HTTPS。主机可使用以下核心代理配置（域名与证书路径需要替换）：

```nginx
server {
    listen 80;
    server_name health.example.com;
    return 301 https://$host$request_uri;
}
server {
    listen 443 ssl;
    server_name health.example.com;
    ssl_certificate /etc/letsencrypt/live/health.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/health.example.com/privkey.pem;
    client_max_body_size 6m;
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $http_host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $http_host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

容器 Nginx 需保留主机提供的 HTTPS 协议头，参见随附 nginx.conf；后端只对该内部代理网络可达。使用同源 /api，无需向公网开放后端或放宽所有 CORS 域名。

上线验收：注册登录、同一账号两设备记录同步、中文/英文发送、旧记录导入、超过 80 条分页、编辑冲突、备份恢复、证书自动续期，以及服务重启后的任务状态。监测进程健康、磁盘空间、备份时间和聊天生成失败。
