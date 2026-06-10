package com.example.demo.config;

import com.example.demo.machine.MachineStatusChangeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, MachineStatusChangeEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // нужен для Instant в событии

        Serializer<MachineStatusChangeEvent> valueSerializer = (topic, data) -> {
            try {
                return mapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Ошибка сериализации события", e);
            }
        };

        var factory = new DefaultKafkaProducerFactory<String, MachineStatusChangeEvent>(config);
        factory.setValueSerializer(valueSerializer);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, MachineStatusChangeEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}