# 快速开始（Quick Start）

本指南用于在本地快速启动 `ComicsAI` 的完整开发环境（后端 + 用户端 + 管理端）。

> English: This guide helps you bootstrap the full ComicsAI stack locally, including backend APIs, reader app, and admin portal.

## 目录

- [1. 环境要求](#1-环境要求)
- [2. 3 分钟启动（最短路径）](#2-3-分钟启动最短路径)
- [3. 后端配置说明](#3-后端配置说明)
- [4. 前端运行与构建](#4-前端运行与构建)
- [5. 生产部署（Nginx）](#5-生产部署nginx)
- [6. 运行测试](#6-运行测试)
- [7. 常见问题（FAQ）](#7-常见问题faq)

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

不配置也可启动项目，但相关 AI 生成功能会受限。

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

## 5. 生产部署（Nginx）

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

## 6. 运行测试

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

## 7. 常见问题（FAQ）

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
