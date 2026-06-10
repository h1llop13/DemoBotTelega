package com.example.demo;

import jakarta.ws.rs.BeanParam;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic machineStatusEventsTopic() {
        return TopicBuilder.name("machine-status-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
