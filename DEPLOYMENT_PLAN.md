# 🍳 美食推荐系统 - Docker 部署完整计划书

## 一、项目架构分析

### 1.1 技术栈
| 组件 | 技术 | 说明 |
|------|------|------|
| 前端 | Vue 3 + Vite + Element Plus | 端口 5173 (dev) |
| 后端 | Spring Boot 3.2.2 + MyBatis | 端口 8081 |
| 数据库 | MySQL 8.0 | 端口 3306 |
| Python | 定时任务脚本 | 统计数据刷新 |

### 1.2 服务通信
- 前端通过 `/api` 代理访问后端 (nginx 配置)
- 后端直连 MySQL
- Python 脚本直连 MySQL 刷新统计数据

---

## 二、部署架构图

```
┌─────────────────────────────────────────────────────────┐
│                      Linux 服务器                        │
│                                                          │
│   ┌──────────────┐   ┌──────────────┐   ┌───────────┐ │
│   │   Frontend   │   │   Backend    │   │   MySQL   │ │
│   │   (Nginx)    │◄──│ (Spring Boot)│◄──│  8.0      │ │
│   │   :80        │   │   :8081      │   │  :3306    │ │
│   └──────────────┘   └──────────────┘   └───────────┘ │
│         │                   │                  │       │
│         └───────────────────┴──────────────────┘       │
│                         │                              │
│                   Docker Network                       │
└─────────────────────────────────────────────────────────┘
```

---

## 三、文件清单

### 3.1 需要创建的文件

```
项目根目录/
├── docker-compose.yml              # 容器编排 (新建)
├── .env                            # 环境变量配置 (新建)
│
├── backend/
│   └── let-me-cook/
│       ├── Dockerfile              # 后端镜像 (新建)
│       └── application.properties   # 修改: 数据库连接
│
├── frontend/
│   ├── Dockerfile                  # 前端镜像 (新建)
│   └── nginx.conf                  # Nginx配置 (新建)
│
└── mysql/
    └── init/
        ├── 01-schema.sql           # 从 schema.sql 提取
        ├── 02-data.sql             # 从 data.sql 提取
        └── 03-summary-tables.sql   # 汇总表+触发器
```

---

## 四、实施步骤

### 步骤 1: 准备后端配置

**文件**: `backend/let-me-cook/src/main/resources/application.properties`

**修改内容**:
```properties
# 原配置 (localhost)
spring.datasource.url=jdbc:mysql://localhost:3306/food_recommend

# 修改为 (Docker服务名)
spring.datasource.url=jdbc:mysql://mysql:3306/food_recommend
spring.datasource.password=your_docker_db_password
```

---

### 步骤 2: 创建后端 Dockerfile

**文件**: `backend/let-me-cook/Dockerfile`

```dockerfile
# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/let-me-cook-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### 步骤 3: 创建前端 Dockerfile

**文件**: `frontend/Dockerfile`

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

### 步骤 4: 创建 Nginx 配置

**文件**: `frontend/nginx.conf`

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # 前端路由
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api {
        proxy_pass http://backend:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

### 步骤 5: 准备 MySQL 初始化脚本

**目录**: `mysql/init/`

创建以下文件:
1. `01-schema.sql` - 表结构 (从现有 schema.sql 提取)
2. `02-data.sql` - 基础数据 (分类、口味、工艺等)
3. `03-summary-tables.sql` - 汇总表和触发器

---

### 步骤 6: 创建 docker-compose.yml

**文件**: `docker-compose.yml`

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: let-me-cook-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: food_recommend
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql/init:/docker-entrypoint-initdb.d
    command: --default-authentication-plugin=mysql_native_password --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    networks:
      - app-network

  backend:
    build:
      context: ./backend/let-me-cook
      dockerfile: Dockerfile
    container_name: let-me-cook-backend
    restart: always
    ports:
      - "8081:8081"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/food_recommend?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - app-network

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: let-me-cook-frontend
    restart: always
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - app-network

volumes:
  mysql_data:

networks:
  app-network:
    driver: bridge
```

---

### 步骤 7: 创建环境变量文件

**文件**: `.env`

```
DB_PASSWORD=your_secure_password_here
```

---

## 五、部署命令

### 5.1 构建并启动

```bash
# 1. 进入项目目录
cd /path/to/food-recommendation-system

# 2. 构建镜像 (首次或更新代码后)
docker-compose build

# 3. 启动所有服务
docker-compose up -d

# 4. 查看日志
docker-compose logs -f
```

### 5.2 常用命令

```bash
# 查看容器状态
docker-compose ps

# 查看某个服务日志
docker-compose logs -f backend

# 停止服务
docker-compose down

# 重新构建并启动
docker-compose up -d --build

# 进入容器 (调试用)
docker exec -it let-me-cook-backend /bin/sh
docker exec -it let-me-cook-mysql /bin/bash
```

---

## 六、服务器环境要求

### 6.1 硬件要求
| 资源 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 20 GB | 50 GB |

### 6.2 软件要求
- Docker >= 20.10
- Docker Compose >= 2.0

### 6.3 安装 Docker (Ubuntu)

```bash
# 更新apt
sudo apt update && sudo apt upgrade -y

# 安装依赖
sudo apt install -y apt-transport-https ca-certificates curl gnupg lsb-release

# 添加Docker官方GPG密钥
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# 添加Docker仓库
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 启动Docker
sudo systemctl start docker
sudo systemctl enable docker

# 添加当前用户到docker组 (可选)
sudo usermod -aG docker $USER
```

---

## 七、部署检查清单

### 7.1 部署前检查
- [ ] 服务器 Docker 已安装
- [ ] 防火墙已开放端口 80
- [ ] `.env` 文件已配置
- [ ] `application.properties` 数据库地址已修改
- [ ] MySQL 初始化脚本已准备

### 7.2 部署后验证
- [ ] `docker-compose ps` 所有服务 Running
- [ ] 访问 http://服务器IP 显示前端页面
- [ ] 访问 http://服务器IP/api/recipes 测试后端 API
- [ ] 管理员登录功能正常

---

## 八、数据迁移 (可选)

如果需要将本地 MySQL 数据迁移到服务器:

```bash
# 1. 导出本地数据库
mysqldump -u root -p food_recommend > food_recommend.sql

# 2. 复制到服务器
scp food_recommend.sql user@server:/path/

# 3. 导入到Docker MySQL
docker exec -i let-me-cook-mysql mysql -uroot -p food_recommend < food_recommend.sql
```

---

## 九、常见问题

### Q1: 容器启动失败
```bash
# 查看具体错误
docker-compose logs mysql
```

### Q2: 前端无法访问后端
检查 nginx.conf 中的 proxy_pass 配置是否正确

### Q3: 数据库连接失败
- 确认 `.env` 密码正确
- 确认 MySQL 容器已完全启动 (等待 `service_healthy`)

---

## 十、文件创建优先级

| 优先级 | 文件 | 说明 |
|--------|------|------|
| P0 | docker-compose.yml | 核心编排文件 |
| P0 | .env | 环境变量 |
| P0 | backend/Dockerfile | 后端镜像 |
| P0 | frontend/Dockerfile | 前端镜像 |
| P1 | frontend/nginx.conf | 反向代理 |
| P1 | application.properties | 数据库配置 |
| P2 | mysql/init/*.sql | 初始化脚本 |

---

**计划制定完成时间**: 2026-03-08
