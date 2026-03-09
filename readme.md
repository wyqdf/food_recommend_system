# 美食推荐系统

一个完整的美食推荐系统，包含用户端、管理后台和后端服务。

## 技术栈

- **后端**: Spring Boot 4.0.3 + MyBatis + MySQL
- **前端**: Vue 3 + Element Plus + Pinia + Axios
- **数据库**: MySQL 8.0
- **认证**: JWT Token

## 项目结构

```
food-recommendation-system/
├── backend/              # 后端项目
│   ├── let-me-cook/          # Spring Boot 主项目
│   └── 管理员 API 设计文档.md   # API 设计文档
├── frontend/             # 前端项目（用户端）
├── admin/                # 管理后台前端
└── dataset/              # 数据集目录
```

## 功能模块

### 用户端功能
- 菜谱浏览、搜索、筛选
- 用户注册、登录
- 收藏、评论互动
- 个性化推荐

### 管理后台功能
- **数据统计**: 用户、菜谱、评论等数据统计
- **用户管理**: 用户增删改查、状态控制
- **食谱管理**: 食谱增删改查、审核
- **分类管理**: 分类增删改查
- **系统日志**: 管理员操作日志

## 快速启动

### 后端启动

```bash
cd backend/let-me-cook
mvn spring-boot:run
```

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

### 管理后台访问

- 访问地址：http://localhost:5173/admin/login
- 测试账号：admin / admin123

## 数据库配置

```yaml
host: localhost:3306
database: food_recommend
user: root
password: 123456
```

## 文档说明

### 设计文档
- [管理员 API 设计文档](backend/管理员 API 设计文档.md) - 完整的 API 接口设计（26 个接口）
- [管理员功能实现进度](backend/管理员 API 实现进度.md) - 实现进度跟踪
- [前端 API 文档](frontend/API 文档.md) - 用户端 API 文档

### 使用说明
- [管理员功能说明](frontend/管理员功能说明.md) - 管理后台使用说明
- [管理员功能实现总结](管理员功能实现总结.md) - 功能实现总结
- [项目总结](项目总结.md) - 项目整体说明

### 数据库文档
- [数据库修改记录](数据库修改相关操作/修改记录.log) - 数据库变更历史

## 开发进度

### 已完成
- ✅ 数据库设计与创建（含 600 万 + 数据）
- ✅ 管理员数据库表更新（admins, system_logs）
- ✅ 后端项目初始化
- ✅ 前端项目初始化
- ✅ 用户端 API 开发（16 个接口）
- ✅ 管理员 API 后端实现（26 个接口）
- ✅ 管理后台前端开发（6 个页面）
- ✅ 前后端联调

## 重要提示

!!!
要极其重视文档的编写和维护，文档要及时更新，要严格依赖文档中的内容，不能脱离文档而进行修改

对代码的修改都要记录下修改的时间、内容、原因

内容的修改要同步更新到各个相关文档中，例如 API 文档、数据库文档等
