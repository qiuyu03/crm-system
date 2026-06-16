package com.crm.notify.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange 名称
    public static final String EXCHANGE = "crm.topic";

    // 路由键
    public static final String KEY_PRODUCTION_ALERT  = "order.production.alert";
    public static final String KEY_LOGISTICS_ALERT   = "order.logistics.alert";
    public static final String KEY_ORDER_COMPLETED   = "order.completed";

    // 队列名称
    public static final String QUEUE_PRODUCTION_ALERT = "queue.production.alert";
    public static final String QUEUE_LOGISTICS_ALERT  = "queue.logistics.alert";
    public static final String QUEUE_ORDER_COMPLETED  = "queue.order.completed";

    @Bean
    TopicExchange crmExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue productionAlertQueue() {
        return QueueBuilder.durable(QUEUE_PRODUCTION_ALERT).build();
    }

    @Bean
    Queue logisticsAlertQueue() {
        return QueueBuilder.durable(QUEUE_LOGISTICS_ALERT).build();
    }

    @Bean
    Queue orderCompletedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_COMPLETED).build();
    }

    @Bean
    Binding productionAlertBinding() {
        return BindingBuilder.bind(productionAlertQueue()).to(crmExchange()).with(KEY_PRODUCTION_ALERT);
    }

    @Bean
    Binding logisticsAlertBinding() {
        return BindingBuilder.bind(logisticsAlertQueue()).to(crmExchange()).with(KEY_LOGISTICS_ALERT);
    }

    @Bean
    Binding orderCompletedBinding() {
        return BindingBuilder.bind(orderCompletedQueue()).to(crmExchange()).with(KEY_ORDER_COMPLETED);
    }

    // 消息使用 JSON 序列化
    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
