# 快速开始（Quick Start）

本指南用于在本地快速启动 `ComicsAI` 的完整开发环境（后端 + 用户端 + 管理端）。

> English: This guide helps you bootstrap the full ComicsAI stack locally, including backend APIs, reader app, and admin portal.

## 目录

- [1. 环境要求](#1-环境要求)
- [2. 3 分钟启动（最短路径）](#2-3-分钟启动最短路径)
- [3. 后端配置说明](#3-后端配置说明)
- [4. 前端运行与构建](#4-前端运行与构建)
- [5. AI 对接与首条内容生成（必做）](#5-ai-对接与首条内容生成必做)
- [6. 生产部署（Nginx）](#6-生产部署nginx)
- [7. 运行测试](#7-运行测试)
- [8. 常见问题（FAQ）](#8-常见问题faq)

## 约定术语

- 用户端：`reader-app`
- 管理端：`admin-portal`
- 后端：`backend`

## 1. 环境要求

| 工具 | 版本要求 | 作用 |
|---|---|---|
| Java | 17+ | 后端运行环境 |
| Maven | 3.8+ | 后端构建与测试 |
| Node.js | 18+ | 前端构建与开发服务 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7+ | 缓存与加速 |
| Nginx | 1.20+（可选） | 生产反向代理 |

## 2. 3 分钟启动（最短路径）

### Step 1: 创建数据库

```sql
CREATE DATABASE comics_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 表结构由 Flyway 在后端启动时自动迁移，无需手动执行 SQL 文件。

### Step 2: 启动后端

编辑 `backend/src/main/resources/application.yml`，至少确认以下配置：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.data.redis.host`
- `spring.data.redis.port`

并准备 AI Key（至少配置一个文本模型）：

```bash
export OPENAI_API_KEY="your-openai-key"
export QWEN_API_KEY="your-qwen-key"
```

启动后端：

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

### Step 3: 启动用户端

```bash
cd reader-app
npm install
npm run dev
```

默认地址：`http://localhost:5173`

### Step 4: 启动管理端

```bash
cd admin-portal
npm install
npm run dev
```

默认地址：`http://localhost:5174`

## 3. 后端配置说明

配置文件：`backend/src/main/resources/application.yml`

### 3.1 数据库与 Redis

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/comics_ai?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: root

  data:
    redis:
      host: localhost
      port: 6379
      password:
```

### 3.2 文件存储目录

```yaml
app:
  file-storage:
    base-path: ./uploads
```

请确保目录可写：

```bash
mkdir -p ./uploads
```

### 3.3 AI Provider 配置

默认读取环境变量：

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `QWEN_API_KEY`
- `QWEN_BASE_URL`

支持的 Provider 名称（用于故事线生成配置）：

- 文本：`openai`、`qwen`
- 图片：`dall-e`、`wanxiang`

常见模型示例：

- OpenAI 文本：`gpt-4`（或你账号可用的其他模型）
- Qwen 文本：`qwen-turbo`
- DALL-E 图片：`dall-e-3`

如果完全不配置 Key，系统仍可启动，但 AI 内容生成会失败。

### 3.4 打包运行（可选）

```bash
cd backend
mvn clean package -DskipTests
java -jar target/comics-ai-backend-0.0.1-SNAPSHOT.jar
```

## 4. 前端运行与构建

### 4.1 用户端（reader-app）

开发：

```bash
cd reader-app
npm install
npm run dev
```

构建：

```bash
cd reader-app
npm run build
```

### 4.2 管理端（admin-portal）

开发：

```bash
cd admin-portal
npm install
npm run dev
```

构建：

```bash
cd admin-portal
npm run build
```

### 4.3 API 前缀约定

- 用户端：`/api/reader/*`
- 管理端：`/api/admin/*`

## 5. AI 对接与首条内容生成（必做）

这一节是“把大模型真正跑起来”的关键流程。

### 5.1 最小对接清单

1. 至少配置一个文本 Provider 的 Key（`OPENAI_API_KEY` 或 `QWEN_API_KEY`）；
2. 若生成漫画，建议同时配置图片 Provider Key；
3. 创建故事线并配置生成参数；
4. 将故事线状态切到 `ENABLED`，等待定时任务生成；
5. 审核通过后等待发布或按运营流程上线。

### 5.2 管理端方式（推荐）

1. 打开管理端：`http://localhost:5174`
2. 使用默认管理员登录：
   - 账号：`admin@comicsai.com`
   - 密码：`admin123456`
3. 新建故事线（题材、角色设定、世界观、剧情大纲）
4. 在“生成配置”里设置：
   - `textProvider`：`openai` 或 `qwen`
   - `textModel`：如 `gpt-4` / `qwen-turbo`
   - `imageProvider`：`dall-e` 或 `wanxiang`（小说可留空）
   - `imageModel`：如 `dall-e-3`
   - `temperature`、`maxTokens`、`imageStyle`、`imageSize`
5. 启用故事线（状态改为 `ENABLED`）

### 5.3 API 方式（可脚本化）

如果你希望自动化验收，可以直接调接口。

1) 管理员登录

```bash
curl -X POST "http://localhost:8080/api/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@comicsai.com","password":"admin123456"}'
```

> 从返回 JSON 中取出 `token`，后续请求放到 `Authorization: Bearer <token>`。

2) 创建故事线

```bash
curl -X POST "http://localhost:8080/api/admin/storylines" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title":"星海遗民",
    "genre":"科幻冒险",
    "contentType":"NOVEL",
    "characterSettings":"主角林澈，前星舰工程师，性格冷静但执拗。",
    "worldview":"人类文明崩塌后，星际殖民地各自为政。",
    "plotOutline":"主角寻找失落母舰坐标，逐步揭开文明覆灭真相。"
  }'
```

3) 配置生成参数（将 `<storylineId>` 替换为上一步返回 id）

```bash
curl -X PUT "http://localhost:8080/api/admin/storylines/<storylineId>/generation-config" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "textProvider":"qwen",
    "textModel":"qwen-turbo",
    "imageProvider":"wanxiang",
    "imageModel":"wanx2.1-t2i-turbo",
    "temperature":0.7,
    "maxTokens":2000,
    "imageStyle":"anime",
    "imageSize":"1024x1024"
  }'
```

4) 启用故事线

```bash
curl -X PUT "http://localhost:8080/api/admin/storylines/<storylineId>/status" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"status":"ENABLED"}'
```

5) 查看生成结果（管理侧）

```bash
curl -X GET "http://localhost:8080/api/admin/contents?page=1&size=10" \
  -H "Authorization: Bearer <token>"
```

6) 审核通过（将 `<contentId>` 换成待审核内容 id）

```bash
curl -X PUT "http://localhost:8080/api/admin/contents/<contentId>/review" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"action":"approve"}'
```

### 5.4 生成与发布时序说明（非常重要）

- 定时生成：每天 `02:00`（生成后状态通常为 `PENDING_REVIEW`）
- 审核通过后：状态进入 `PENDING_PUBLISH`
- 定时发布：每天 `06:00`（发布后用户端列表可见）

如果你想本地快速验收，不想等到凌晨：

- 临时修改调度类里的 Cron 表达式后重启后端：
  - `backend/src/main/java/com/comicsai/scheduler/ContentGenerationJob.java`
  - `backend/src/main/java/com/comicsai/scheduler/ContentPublishingJob.java`

### 5.5 成功验收标准

满足以下任一组即可判定 AI 已对接成功：

- 管理端能看到新生成内容，且可查看封面/章节/分镜；
- `token_usage` 表出现对应 `provider_name` 和 `model_name` 记录；
- 用户端接口 `GET /api/reader/contents` 能查到已发布内容。

## 6. 生产部署（Nginx）

1. 构建两个前端应用；
2. 部署后端 JAR；
3. 使用根目录 `nginx.conf` 代理静态资源和 API；
4. 配置上传目录权限。

示例命令：

```bash
# 构建前端
cd reader-app && npm run build
cd ../admin-portal && npm run build

# 启动后端（示例）
cd ../backend && java -jar target/comics-ai-backend-0.0.1-SNAPSHOT.jar

# 使用项目 nginx 配置（按实际路径调整）
nginx -c /path/to/comicsai/nginx.conf
```

## 7. 运行测试

### Backend

```bash
cd backend
mvn test
```

### Reader App

```bash
cd reader-app
npm run test
```

## 8. 常见问题（FAQ）

### Q1: 后端启动时报 Flyway 相关错误？

- 确认 MySQL 已启动；
- 确认数据库 `comics_ai` 已创建；
- 确认 `application.yml` 中用户名和密码正确。

### Q2: 前端接口请求 401 / 404？

- 确认后端已经启动；
- 确认请求路径使用对应前缀：
  - 用户端：`/api/reader`
  - 管理端：`/api/admin`

### Q3: 文件上传失败？

- 检查 `app.file-storage.base-path` 目录是否存在；
- 检查目录写权限是否正确。

### Q4: 定时生成/发布没有触发？

默认 Cron：

- 内容生成：每天 `02:00`
- 内容发布：每天 `06:00`

开发调试可以临时调整定时任务表达式，或通过管理端手动处理内容流程。

### Q5: 明明配了 AI Key，还是生成失败？

- 确认故事线 `generation-config` 中的 `textProvider` / `imageProvider` 与系统支持值一致：
  - 文本：`openai`、`qwen`
  - 图片：`dall-e`、`wanxiang`
- 确认模型名在对应平台账号下可用；
- 查看后端日志中 `AI provider failed`、`All text AI providers failed` 等报错。

### Q6: 管理端登录不上？

- 确认 Flyway 已执行到 `V2__add_admin_and_oauth.sql`；
- 使用默认管理员账号：
  - `admin@comicsai.com` / `admin123456`
