package com.example.demo.service;

import com.example.demo.machine.MachineStatusChangeEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MachineEventConsumer {
    private final MachineEventService machineEventService;

    public MachineEventConsumer(MachineEventService machineEventService) {
        this.machineEventService = machineEventService;
    }

    @KafkaListener(topics = "machine-status-events", groupId = "machine-bot-group")
    public void consume(MachineStatusChangeEvent event) {
        System.out.println(">>> Kafka получила событие: " + event.machineId());
        machineEventService.process(event);
    }
}
