# 实现计划：AI漫画与小说阅读平台

## 概述

基于设计文档，将实现分为后端（Java Spring Boot）和前端（Vue 3 + TypeScript）两大部分，按模块逐步推进。每个任务构建在前一个任务之上，确保代码始终可运行。

## 任务

- [x] 1. 项目初始化与基础架构搭建
  - [x] 1.1 创建Spring Boot后端项目骨架
    - 使用Spring Boot 3 + Java 17初始化项目
    - 配置Maven依赖：Spring Web、MyBatis-Plus、MySQL Driver、Redis、jqwik
    - 创建包结构：controller/reader、controller/admin、service、ai、scheduler、model/entity、model/dto、model/vo、mapper、config、common
    - 配置application.yml（数据库、Redis、文件存储路径）
    - 实现全局异常处理器（ApiResponse统一响应格式、@RestControllerAdvice）
    - _Requirements: 18.1, 18.5_

  - [x] 1.2 创建Vue 3前端项目骨架（浏览端Reader App）
    - 使用Vite + Vue 3 + TypeScript初始化项目
    - 配置Pinia状态管理、Vue Router（路由懒加载）
    - 创建目录结构：views、components、stores、api、router、styles
    - 配置全局样式（现代极简风格基础变量：配色、字体、间距）
    - 封装Axios请求工具（统一错误处理、Token拦截器）
    - _Requirements: 6.1, 6.4, 19.7_

  - [x] 1.3 创建Vue 3前端项目骨架（管理端Admin Portal）
    - 使用Vite + Vue 3 + TypeScript初始化管理端项目
    - 配置Pinia、Vue Router、Element Plus或类似UI组件库
    - 创建目录结构：views、components、stores、api、router
    - 封装Axios请求工具
    - _Requirements: 无直接需求，基础设施_

- [x] 2. 数据模型与数据库
  - [x] 2.1 创建MySQL数据库表和MyBatis-Plus实体类
    - 创建所有数据库表：user、storyline、storyline_version、generation_config、content、comic_page、novel_chapter、view_event、token_usage、recharge_record、content_unlock
    - 编写对应的Entity类和Mapper接口
    - 编写Flyway/Liquibase数据库迁移脚本
    - _Requirements: 18.1, 18.3_

  - [~]* 2.2 编写数据模型属性测试
    - **Property 36: 分页查询正确性**
    - **Validates: Requirements 18.4**

- [ ] 3. 用户认证模块
  - [x] 3.1 实现用户注册、登录、登出API
    - 实现UserService：注册（邮箱唯一校验、密码加密）、登录（JWT Token生成）、登出（Redis会话清除）
    - 实现UserController（/api/reader/auth/*）
    - 实现JWT Token拦截器和权限注解
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [~]* 3.2 编写用户认证属性测试
    - **Property 7: 用户注册创建账号**
    - **Validates: Requirements 4.4**
    - **Property 8: 登出清除会话**
    - **Validates: Requirements 4.6**

  - [x] 3.3 实现浏览端登录/注册页面
    - 创建LoginPage.vue（登录表单、注册表单切换）
    - 实现Pinia用户状态管理（token存储、登录状态）
    - 实现路由守卫（付费内容访问拦截）
    - _Requirements: 4.2, 4.4, 6.1_

- [x] 4. Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请向用户确认。

- [ ] 5. 内容管理核心模块（后端）
  - [x] 5.1 实现ContentService核心逻辑
    - 实现内容CRUD操作
    - 实现内容状态流转逻辑（状态机校验）
    - 实现内容分页查询（支持类型、状态、Storyline筛选）
    - 实现文件存储服务（图片/文本文件的保存和读取）
    - _Requirements: 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 18.2, 18.3, 18.4_

  - [~]* 5.2 编写内容状态流转属性测试
    - **Property 13: 内容状态流转正确性**
    - **Validates: Requirements 9.3, 9.4, 9.6, 9.7**

  - [x] 5.3 实现浏览端内容API
    - 实现ContentController（/api/reader/contents/*）
    - 实现内容列表查询（按发布时间倒序、分页、类型筛选）
    - 实现内容详情查询（Comic页面列表、Novel章节列表）
    - 实现搜索API（关键词匹配标题和简介）
    - 实现Redis缓存（首页列表、热门内容、内容详情）
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 18.5_

  - [~]* 5.4 编写内容查询属性测试
    - **Property 1: 内容列表按发布时间倒序排列**
    - **Validates: Requirements 1.1**
    - **Property 2: 内容列表响应数据完整性**
    - **Validates: Requirements 1.4, 10.3**
    - **Property 3: 内容类型筛选正确性**
    - **Validates: Requirements 2.1**
    - **Property 4: 关键词搜索正确性**
    - **Validates: Requirements 2.2**
    - **Property 5: 空白搜索关键词拒绝**
    - **Validates: Requirements 2.4**

  - [x] 5.5 实现管理端内容管理API
    - 实现ContentManageController（/api/admin/contents/*）
    - 实现内容审核（单个和批量）
    - 实现内容编辑、上下架
    - 实现内容列表筛选（状态、类型、Storyline、付费状态）
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9_

  - [~]* 5.6 编写内容管理属性测试
    - **Property 14: 内容管理列表筛选正确性**
    - **Validates: Requirements 9.8, 10.4**
    - **Property 15: 批量操作一致性**
    - **Validates: Requirements 9.9, 10.5**

- [ ] 6. 访问控制与付费模块
  - [x] 6.1 实现访问控制逻辑
    - 在ContentService中实现内容访问权限校验
    - Guest可访问免费内容，Paid_Content需登录且已解锁
    - 实现内容解锁API（余额校验、扣费、记录）
    - _Requirements: 4.1, 4.2, 4.3, 5.3, 5.4_

  - [~]* 6.2 编写访问控制属性测试
    - **Property 6: 访问控制正确性**
    - **Validates: Requirements 4.1, 4.2, 4.3**

  - [x] 6.3 实现充值与余额管理
    - 实现BalanceService：充值（增加余额、记录充值）、扣费（解锁付费内容）
    - 实现BalanceController（/api/reader/user/recharge、/api/reader/contents/{id}/unlock）
    - _Requirements: 5.2, 5.3, 5.4, 5.5_

  - [~]* 6.4 编写充值与余额属性测试
    - **Property 9: 充值余额正确性**
    - **Validates: Requirements 5.2**
    - **Property 10: 付费内容解锁与余额扣除**
    - **Validates: Requirements 5.3, 5.4**

  - [x] 6.5 实现管理端付费内容管理API
    - 在ContentManageController中添加付费设置接口
    - 实现单个和批量设置付费属性和价格
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [~]* 6.6 编写付费属性测试
    - **Property 16: 付费属性设置往返一致性**
    - **Validates: Requirements 10.1, 10.2**

- [x] 7. Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请向用户确认。

- [ ] 8. 故事线管理模块
  - [x] 8.1 实现StorylineService
    - 实现故事线CRUD（创建、编辑、查询、列表）
    - 实现Storyline_Template必填字段验证
    - 实现故事线版本管理（编辑时保存历史版本快照）
    - 实现故事线状态切换（启用/停用）
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [~]* 8.2 编写故事线属性测试
    - **Property 11: 故事线模板验证**
    - **Validates: Requirements 7.1, 7.2**
    - **Property 12: 故事线编辑版本记录**
    - **Validates: Requirements 7.4**

  - [x] 8.3 实现Generation_Config管理
    - 实现GenerationConfig的CRUD
    - 实现Storyline与Generation_Config的绑定关系
    - 实现StorylineController（/api/admin/storylines/*）
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [~]* 8.4 编写生成配置属性测试
    - **Property 25: 生成配置绑定正确性**
    - **Validates: Requirements 8.3**

- [ ] 9. AI对接与内容生成模块
  - [x] 9.1 实现AI Provider统一接口和适配器
    - 定义TextAiProvider和ImageAiProvider接口
    - 实现OpenAI适配器（OpenAiTextProvider、DallEImageProvider）
    - 实现通义千问适配器（QwenTextProvider、WanxiangImageProvider）
    - 实现AiProviderFactory（根据配置创建Provider实例）
    - 实现备用Provider故障降级逻辑
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6_

  - [~]* 9.2 编写AI Provider属性测试
    - **Property 32: AI_Provider故障降级**
    - **Validates: Requirements 16.6**

  - [x] 9.3 实现ContentGeneratorService
    - 实现基于Storyline的提示词构建（角色设定、世界观、剧情大纲、前章摘要）
    - 实现Comic生成流程（文本AI生成分镜脚本→图片AI生成漫画图片）
    - 实现Novel生成流程（文本AI生成章节内容）
    - 实现章节摘要自动生成与存储
    - 实现Token消耗记录
    - 实现重试机制（失败后30分钟重试，最多3次）
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 15.1, 15.2, 15.3, 15.4, 15.5, 12.1_

  - [ ]* 9.4 编写内容生成属性测试
    - **Property 24: 内容生成仅处理启用的故事线**
    - **Validates: Requirements 7.5, 7.6, 14.1**
    - **Property 26: 生成内容完整性**
    - **Validates: Requirements 14.3, 14.4**
    - **Property 27: 生成内容初始状态正确性**
    - **Validates: Requirements 14.2**
    - **Property 28: AI调用重试机制**
    - **Validates: Requirements 14.5**
    - **Property 29: 提示词构建包含故事线要素**
    - **Validates: Requirements 15.1**
    - **Property 30: 章节连贯性上下文传递**
    - **Validates: Requirements 15.2**
    - **Property 31: 章节摘要自动生成与存储**
    - **Validates: Requirements 15.5**
    - **Property 20: Token消耗记录完整性**
    - **Validates: Requirements 12.1**

- [ ] 10. 定时调度模块
  - [x] 10.1 实现Scheduler定时任务
    - 实现ContentGenerationJob（Cron: 0 0 2 * * ?，每天凌晨2:00触发内容生成）
    - 实现ContentPublishingJob（Cron: 0 0 6 * * ?，每天早上6:00触发内容发布）
    - 实现PublishingService（批量更新待发布→已发布，清除相关缓存）
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5_

  - [ ]* 10.2 编写发布任务属性测试
    - **Property 33: 发布任务状态批量更新**
    - **Validates: Requirements 17.2**
    - **Property 34: 已发布内容可见性**
    - **Validates: Requirements 17.3**

- [x] 11. Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请向用户确认。

- [ ] 12. 统计分析模块
  - [x] 12.1 实现用户使用统计
    - 实现AnalyticsService：记录查看事件、记录阅读时长
    - 实现查看事件上报API（/api/reader/analytics/view、/api/reader/analytics/duration）
    - 实现用户使用统计查询API（/api/admin/analytics/usage）
    - 支持时间范围、内容类型、付费状态筛选
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

  - [ ]* 12.2 编写用户使用统计属性测试
    - **Property 17: 查看事件记录完整性**
    - **Validates: Requirements 11.3**
    - **Property 18: 阅读留存时长计算正确性**
    - **Validates: Requirements 11.4**
    - **Property 19: 统计数据时间范围过滤正确性**
    - **Validates: Requirements 11.5, 12.4, 13.3**

  - [x] 12.3 实现Token消耗成本统计
    - 实现Token消耗统计查询API（/api/admin/analytics/token-cost）
    - 支持按AI_Provider/模型维度和Storyline维度聚合
    - 支持时间范围筛选和每日趋势数据
    - _Requirements: 12.2, 12.3, 12.4, 12.5, 12.6_

  - [ ]* 12.4 编写Token消耗统计属性测试
    - **Property 21: Token消耗聚合正确性**
    - **Validates: Requirements 12.2, 12.3**

  - [x] 12.5 实现充值用户统计
    - 实现充值统计查询API（/api/admin/analytics/recharge）
    - 实现充值汇总（总数、总金额、平均金额）
    - 实现充值用户列表和付费内容消费统计
    - 支持时间范围筛选
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_

  - [ ]* 12.6 编写充值统计属性测试
    - **Property 22: 充值统计聚合正确性**
    - **Validates: Requirements 13.1**
    - **Property 23: 付费内容消费统计正确性**
    - **Validates: Requirements 13.5**

- [ ] 13. 浏览端前端页面实现
  - [x] 13.1 实现首页与内容列表
    - 创建HomePage.vue（内容卡片网格布局、分类标签切换）
    - 创建ContentCard.vue（封面图、标题、类型标签、发布日期、付费标识）
    - 实现InfiniteScroll.vue（无限滚动加载、加载指示器、加载完毕提示）
    - 实现SkeletonLoader.vue（骨架屏占位）
    - 实现图片懒加载
    - _Requirements: 1.1, 1.4, 3.1, 3.2, 3.3, 6.1, 19.2, 19.8_

  - [x] 13.2 实现搜索页面
    - 创建SearchPage.vue（搜索框、搜索结果列表、空结果推荐）
    - 实现空白关键词校验（前端拦截）
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 13.3 实现漫画阅读器
    - 创建ComicReader.vue（翻页模式、图片展示、对话文本）
    - 实现翻页动画和阅读进度保持
    - 实现图片预加载（当前页 + 前后各一页）
    - 实现查看事件和阅读时长上报
    - _Requirements: 1.2, 1.5, 11.3, 11.4, 19.3_

  - [x] 13.4 实现小说阅读器
    - 创建NovelReader.vue（章节阅读模式、章节导航）
    - 实现查看事件和阅读时长上报
    - _Requirements: 1.3, 11.3, 11.4_

  - [x] 13.5 实现个人中心与充值页面
    - 创建ProfilePage.vue（用户信息、余额、充值消费记录）
    - 创建RechargePage.vue（充值金额选项、确认支付）
    - 创建PaywallModal.vue（付费提示弹窗、余额不足引导）
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 13.6 实现响应式布局与页面过渡
    - 配置全局响应式断点和媒体查询
    - 实现Vue Router页面过渡动画
    - 实现Comic图片多尺寸适配（srcset）
    - _Requirements: 6.2, 6.3, 19.5_

- [ ] 14. 管理端前端页面实现
  - [x] 14.1 实现故事线管理页面
    - 创建StorylineManage.vue（故事线列表、创建/编辑表单、状态切换）
    - 实现Storyline_Template表单（题材类型、角色设定、世界观、剧情大纲）
    - 实现Generation_Config配置面板（AI模型选择、参数设置）
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 8.1, 8.2_

  - [x] 14.2 实现内容管理页面
    - 创建ContentManage.vue（内容列表、筛选栏、批量操作）
    - 创建ContentReview.vue（内容详情预览、审核操作）
    - 实现内容编辑功能（标题、封面图、文本内容修改）
    - 实现上下架和付费设置功能
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 10.1, 10.2, 10.3, 10.4, 10.5_

  - [x] 14.3 实现用户使用统计看板
    - 创建UserAnalytics.vue（查看人数统计、留存时长统计）
    - 实现ChartPanel.vue（基于ECharts的图表组件）
    - 实现时间范围选择器和筛选栏
    - _Requirements: 11.1, 11.2, 11.5, 11.6, 11.7_

  - [x] 14.4 实现Token消耗成本看板
    - 创建TokenAnalytics.vue（Token消耗汇总、成本曲线、明细列表）
    - 实现按Provider/模型和Storyline维度的数据展示
    - _Requirements: 12.2, 12.3, 12.4, 12.5, 12.6_

  - [x] 14.5 实现充值用户统计看板
    - 创建RechargeAnalytics.vue（充值汇总、趋势图、用户列表、消费统计）
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_

- [ ] 15. 集成与性能优化
  - [x] 15.1 前后端联调与集成
    - 配置CORS跨域
    - 配置Nginx反向代理（API转发、静态资源服务、文件存储访问）
    - 验证浏览端和管理端所有页面与后端API的联通性
    - _Requirements: 19.4_

  - [x] 15.2 实现元数据与文件资源关联校验
    - 实现文件存储健康检查
    - 确保Content元数据中的URL指向有效文件
    - _Requirements: 18.3_

  - [ ]* 15.3 编写文件关联属性测试
    - **Property 35: 元数据与文件资源关联正确性**
    - **Validates: Requirements 18.3**

- [x] 16. 最终Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请向用户确认。

## 备注

- 标记 `*` 的任务为可选任务，可跳过以加速MVP开发
- 每个任务引用了具体的需求编号，确保可追溯性
- Checkpoint任务用于阶段性验证，确保增量开发的稳定性
- 属性测试验证通用正确性属性，单元测试验证具体示例和边界情况