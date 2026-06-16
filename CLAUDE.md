# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

轻量级 CRM + 订单全链路系统，覆盖"获客-转化-履约-复购"生命周期。单体 Spring Boot 架构，核心模块：客户分级管理、订单跟踪、异步预警。

## 常用命令

```bash
# 编译（不运行测试）
mvn compile

# 打包
mvn package -DskipTests

# 启动后端
mvn spring-boot:run

# 健康检查（项目启动后）
curl http://localhost:8080/actuator/health

# 重建数据库表（MySQL 客户端执行）
source src/main/resources/sql/init.sql

# 前端（在 crm-frontend/ 目录）
npm install
npm run dev
```

## 本地中间件启动

Redis 和 RabbitMQ 通过 Docker 启动：

```bash
docker run -d -p 6379:6379 redis
docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:management
```

RabbitMQ 管理界面：http://localhost:15672（默认 admin/admin123）

## 配置

`application.yml` 含真实密码，已加入 `.gitignore` 不提交。  
新环境参照 `src/main/resources/application-example.yml` 创建 `application.yml`。

## 架构与数据流

### 包结构约定

每个业务模块（`customer` / `order` / `notify`）内部按 `entity → mapper → service → controller` 分层，跨模块依赖只允许 service 层之间互调（如 `CustomerService` 被 `AlertConsumer` 调用）。

### 异步预警数据流

```
OrderService.changeStatus("已完成")
  └─ rabbitTemplate.send(EXCHANGE, KEY_ORDER_COMPLETED)
       └─ AlertConsumer.handleOrderCompleted()
            └─ CustomerService.onOrderCompleted()   ← 更新冗余字段 + 重新评分

OrderService.triggerAlert()
  └─ rabbitTemplate.send(EXCHANGE, KEY_PRODUCTION_ALERT / KEY_LOGISTICS_ALERT)
       └─ AlertConsumer.processAlert()
            ├─ alertMapper.insert()                 ← 持久化
            ├─ FeishuNotifyService.sendAlert()      ← 飞书 Webhook 推送
            └─ WebSocketNotifyService.pushAlert()   ← STOMP 广播前端
```

RabbitMQ 使用 Topic Exchange（`crm.topic`），所有路由键和队列名定义在 `RabbitMQConfig` 常量中。消费者全部使用**手动 ACK**（`basicAck` / `basicNack + requeue`）。

### 客户评分机制

`CustomerScoringService` 实现三维度加权评分（总分 100）：
- 金额维度（40分）：`totalAmount` 字段，5档阈值
- 频率维度（35分）：`orderCount` 字段，4档阈值
- 信用维度（25分）：`overdueCount` + `creditDays` 字段

`t_customer` 上的 `score`、`total_amount`、`order_count` 是**冗余字段**，不实时计算——由订单完成 MQ 事件异步触发 `CustomerService.onOrderCompleted()` 更新，再调 `recalculateScore()` 写回。

### MyBatis-Plus 使用约定

- 简单单表查询用 `LambdaQueryWrapper` / `LambdaUpdateWrapper` 在 Service 层内联编写，不新建 Mapper 方法。
- 动态条件分页或 `IN + LIMIT` 等复杂 SQL 写在 `src/main/resources/mapper/*.xml`，对应 Mapper 接口新增方法签名。
- 分页对象统一使用 `com.baomidou.mybatisplus.extension.plugins.pagination.Page`，页码**从 1 开始**（MP 规范）。
- `createTime` / `updateTime` 由 `MyMetaObjectHandler` 自动填充，实体上标注 `@TableField(fill = FieldFill.INSERT)` / `INSERT_UPDATE`，无需手动赋值。

### WebSocket

使用 STOMP over SockJS，端点 `/ws`。  
- 前端订阅 `/topic/orders/{orderId}/status` 获取订单实时状态变更。  
- 前端订阅 `/topic/alerts` 获取全局预警通知。  
- 服务端通过 `WebSocketNotifyService` 推送，在订单状态变更和预警消费两处调用。

## 分支策略

- `master`：稳定版本，只接受从 `test` merge 进来的代码
- `test`：日常开发分支，测试通过后 merge 到 master
