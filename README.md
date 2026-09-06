# Flash Sale（秒杀系统）

面向 Java 后端实习的**单体秒杀**练习项目：把高并发库存、缓存、消息队列与订单可靠性串成一条可讲清的链路。  
不含前端、不做微服务拆分——刻意把深度放在秒杀核心，而不是铺开电商全栈。

## 技术栈

| 类别 | 技术 |
|------|------|
| 基础 | Java 17、Spring Boot 4、Maven |
| 数据 | MySQL 8、MyBatis-Plus、Flyway |
| 缓存 | Redis（库存预热、Lua 扣减、限流、防重、缓存模式） |
| 消息 | RabbitMQ（异步下单、confirm/return、重试、死信） |
| 安全 | JWT + 角色（USER / ADMIN）、BCrypt |
| 工程 | Docker Compose、Nginx 反代、Actuator、springdoc OpenAPI、集成测试 |

## 核心能力

- **秒杀链路**：活动校验 → Redis 限流 → Lua 原子扣库存 + 防重复 → 排队结果 → 本地消息表 → MQ → 异步建单 / DB CAS 扣库存
- **缓存**：缓存穿透（空值）、击穿（互斥重建）、逻辑过期异步刷新
- **MQ 可靠性**：发送确认与回退、消费幂等、TTL 重试、死信落库与管理端重投、发送失败定时重试
- **订单**：待支付超时（Redis ZSET）、取消/支付失败回补库存
- **运维向**：Actuator 健康检查、Nginx 统一入口、操作日志 AOP、防重复提交

## 架构示意

```text
客户端
  │
  ├─ 本地开发：http://localhost:8080
  └─ 经 Nginx：http://localhost  (80 → 后端 8080)
        │
        ▼
  Spring Boot
        │
   ┌────┼────┐
   ▼    ▼    ▼
 MySQL Redis RabbitMQ
```

秒杀主路径：

```text
预热库存到 Redis
    → POST /api/activities/{id}/seckill
    → Redis 限流 + Lua（资格 + 扣库存）
    → 写入秒杀结果(排队中) + 本地消息
    → RabbitMQ
    → 消费者落库建单
    → GET /api/activities/{id}/result 查询结果
```

## 快速启动

### 1. 启动依赖

```bash
docker compose up -d mysql redis rabbitmq
```

可选：一并启动 Nginx 反代：

```bash
docker compose up -d nginx
```

| 服务 | 宿主机端口 | 说明 |
|------|------------|------|
| MySQL | **3307** → 容器 3306 | 库名 `flash_sale`，root / 123456 |
| Redis | **16379** → 6379 | |
| RabbitMQ | 5672 / 管理台 15672 | flashsale / flashsale123 |
| Nginx | 80 | 转发到本机 IDEA 的 8080（见 `nginx/default.conf`） |

### 2. 数据库连接注意

Compose 将 MySQL 映射到 **3307**。若 `application-dev.yml` 默认仍是 `localhost:3306`，请启动前设置：

```bash
# Windows PowerShell 示例
$env:DB_URL="jdbc:mysql://localhost:3307/flash_sale?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
```

或在 IDE Run Configuration 里配置同样的 `DB_URL`。

### 3. 启动应用

- IDE 运行 `FlashSaleApplication`（默认 `spring.profiles.active=dev`）
- 或：`./mvnw spring-boot:run`

### 4. 常用地址

| 地址 | 说明 |
|------|------|
| http://localhost:8080/swagger-ui.html | API 文档 |
| http://localhost:8080/api/health | 业务探活 |
| http://localhost:8080/actuator/health | 依赖健康（db / redis / rabbit 等） |
| http://localhost/... | 经 Nginx 访问（需已 up nginx） |

默认管理员（dev 自动初始化）：`admin` / `admin123456`。

登录后在请求头携带：`Authorization: Bearer <token>`。

## 全容器启动（含应用镜像）

```bash
docker compose up -d --build
```

此时应用使用 `prod` profile，环境变量见 `docker-compose.yml` 中 `app` 服务。

若 Nginx 也要转发到 **app 容器**，需将 `nginx/default.conf` 中的：

```nginx
proxy_pass http://host.docker.internal:8080;
```

改为：

```nginx
proxy_pass http://app:8080;
```

（本地 IDEA 开发请继续使用 `host.docker.internal`。）

## 典型业务步骤

1. 注册 / 登录，拿到 JWT  
2. 管理员创建商品、创建秒杀活动  
3. `POST /api/activities/{id}/prepare-stock` 预热库存  
4. 用户 `POST /api/activities/{id}/seckill` 参与秒杀  
5. `GET /api/activities/{id}/result` 查结果；成功后可支付 / 取消订单  

dev 环境另有 `/api/test/**` 压测辅助接口（需管理员）。

## 测试

中间件启动后：

```bash
./mvnw test -Dtest=AuthIntegrationTest
```

或在 IDE 中运行 `src/test/java` 下的集成测试。  
`@SpringBootTest` 会启动完整上下文并连接本机/Docker 中的依赖，**无需**再手动起一遍主程序（注意端口与数据源配置一致）。

## 项目结构

```text
src/main/java/com/study/flashsale
  ├── controller/          # 用户端 + 管理端 API
  ├── service/             # 业务
  ├── mq/                  # 生产者 / 消费者
  ├── infrastructure/      # Redis、缓存封装
  ├── task/                # 超时关单、库存补偿、消息重试
  ├── aspect/              # 操作日志、防重复提交
  └── ...
src/main/resources/db/migration   # Flyway
nginx/default.conf                # Nginx 反代（本地指向宿主机 8080）
docker-compose.yml
Dockerfile
```


## License

学习演示项目，仅供个人练习与面试说明使用。
