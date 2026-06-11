package com.example.demo.service;

import com.example.demo.machine.MachineStatusChangeEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

//@Service
public class MachineEventProducer {

    private static final String TOPIC = "machine-status-events";

    private final KafkaTemplate<String, MachineStatusChangeEvent> kafkaTemplate;

    public MachineEventProducer(KafkaTemplate<String, MachineStatusChangeEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(MachineStatusChangeEvent event) {
        kafkaTemplate.send(TOPIC, event.machineId(), event);
    }
}
