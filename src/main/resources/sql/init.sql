-- CRM 系统核心表结构（MyBatis-Plus 版）
-- 执行方式：先创建库，再执行本文件
--   CREATE DATABASE IF NOT EXISTS crm_db DEFAULT CHARACTER SET utf8mb4;
--   USE crm_db;

USE crm_db;

-- 删除旧表（按外键依赖倒序）
DROP TABLE IF EXISTS t_alert;
DROP TABLE IF EXISTS t_logistics;
DROP TABLE IF EXISTS t_order_item;
DROP TABLE IF EXISTS t_order;
DROP TABLE IF EXISTS t_customer;

-- =============================================
-- 1. 客户表
-- score/total_amount/order_count 为冗余字段，由订单完成 MQ 事件异步维护。
-- =============================================
CREATE TABLE t_customer (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name          VARCHAR(64)   NOT NULL               COMMENT '客户姓名/联系人',
    company       VARCHAR(128)                         COMMENT '公司名称',
    contact       VARCHAR(32)                          COMMENT '联系方式',
    email         VARCHAR(64)                          COMMENT '邮箱',
    level         CHAR(1)       NOT NULL DEFAULT 'C'   COMMENT '等级: S/A/B/C',
    score         INT           NOT NULL DEFAULT 0     COMMENT '综合评分 0-100',
    total_amount  DECIMAL(15,2) NOT NULL DEFAULT 0.00  COMMENT '历史累计订单金额',
    order_count   INT           NOT NULL DEFAULT 0     COMMENT '历史订单笔数',
    credit_days   INT           NOT NULL DEFAULT 30    COMMENT '账期天数',
    overdue_count INT           NOT NULL DEFAULT 0     COMMENT '历史逾期次数',
    remark        VARCHAR(512)                         COMMENT '备注',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_level (level),
    INDEX idx_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';


-- =============================================
-- 2. 订单表
-- delivery_date 作为预警触发时间基准；联合索引加速客户维度分页查询。
-- =============================================
CREATE TABLE t_order (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no      VARCHAR(32)   NOT NULL               COMMENT '订单号（业务唯一键）',
    customer_id   BIGINT        NOT NULL               COMMENT '关联客户ID',
    status        VARCHAR(16)   NOT NULL DEFAULT '待确认'
                  COMMENT '订单状态: 待确认/生产中/已发货/已完成/已取消',
    total_amount  DECIMAL(15,2) NOT NULL DEFAULT 0.00  COMMENT '订单总金额',
    currency      VARCHAR(8)    NOT NULL DEFAULT 'CNY' COMMENT '货币单位',
    delivery_date DATE                                  COMMENT '约定交货日期',
    shipping_date DATETIME                              COMMENT '实际发货时间',
    created_by    VARCHAR(32)                          COMMENT '创建人',
    remark        VARCHAR(512)                         COMMENT '备注',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_customer_id (customer_id),
    INDEX idx_customer_create (customer_id, create_time),
    INDEX idx_status_delivery (status, delivery_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';


-- =============================================
-- 3. 订单明细表
-- =============================================
CREATE TABLE t_order_item (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id      BIGINT        NOT NULL               COMMENT '关联订单ID',
    product_name  VARCHAR(128)  NOT NULL               COMMENT '产品名称',
    product_code  VARCHAR(64)                          COMMENT '产品编号/SKU',
    qty           INT           NOT NULL               COMMENT '数量',
    unit_price    DECIMAL(12,2) NOT NULL               COMMENT '单价',
    amount        DECIMAL(15,2) NOT NULL               COMMENT '小计金额',
    unit          VARCHAR(16)                          COMMENT '单位',
    remark        VARCHAR(256)                         COMMENT '备注',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';


-- =============================================
-- 4. 物流轨迹表（append-only，每次事件新增一行）
-- =============================================
CREATE TABLE t_logistics (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id    BIGINT       NOT NULL               COMMENT '关联订单ID',
    carrier     VARCHAR(32)                         COMMENT '承运商',
    tracking_no VARCHAR(64)                         COMMENT '运单号',
    status      VARCHAR(32)                         COMMENT '轨迹状态',
    location    VARCHAR(128)                        COMMENT '当前位置',
    description VARCHAR(256)                        COMMENT '轨迹描述',
    event_time  DATETIME                            COMMENT '事件发生时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_order_id (order_id),
    INDEX idx_tracking (tracking_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流轨迹表';


-- =============================================
-- 5. 预警记录表
-- notified_at 非空表示飞书已推送，防止重复推送。
-- =============================================
CREATE TABLE t_alert (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id    BIGINT       NOT NULL               COMMENT '关联订单ID',
    alert_type  VARCHAR(32)  NOT NULL               COMMENT '预警类型: 生产异常/物流延误/交期预警',
    message     VARCHAR(512) NOT NULL               COMMENT '预警内容',
    status      VARCHAR(16)  NOT NULL DEFAULT '未处理'
                COMMENT '处理状态: 未处理/已处理',
    handler     VARCHAR(32)                         COMMENT '处理人',
    handle_time DATETIME                            COMMENT '处理时间',
    notified_at DATETIME                            COMMENT '飞书推送时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_type_status (alert_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警记录表';


-- =============================================
-- 初始化测试数据
-- =============================================
INSERT INTO t_customer (name, company, contact, email, level, score, total_amount, order_count, credit_days)
VALUES
('张三', '远景科技有限公司', '13800000001', 'zhangsan@example.com', 'S', 85, 580000.00, 12, 60),
('李四', '蓝海贸易公司',     '13800000002', 'lisi@example.com',     'A', 72, 210000.00,  6, 30),
('王五', '新星实业',         '13800000003', 'wangwu@example.com',   'B', 50,  85000.00,  3, 30),
('赵六', '启明商贸',         '13800000004', 'zhaoliu@example.com',  'C', 25,  15000.00,  1,  0);
