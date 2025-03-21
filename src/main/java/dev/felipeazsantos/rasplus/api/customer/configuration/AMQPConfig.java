package dev.felipeazsantos.rasplus.api.customer.configuration;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AMQPConfig {

    private static final String QUEUE_RECOVERY_CODE_EMAIL = "recovery.code.email";

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Queue createQueue() {
//        return new Queue("recovery.code.email", false);
        return QueueBuilder.nonDurable(QUEUE_RECOVERY_CODE_EMAIL).build();
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> initializeAdmin(RabbitAdmin rabbitAdmin) {
        return event -> rabbitAdmin.initialize();
    }
}
