package com.example.demo.service;

import com.example.demo.machine.MachineStatusChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MachineEventConsumer {
    private final MachineEventService machineEventService;
    private static final Logger log = LoggerFactory.getLogger(MachineEventConsumer.class);

    public MachineEventConsumer(MachineEventService machineEventService) {
        this.machineEventService = machineEventService;
    }

    @KafkaListener(topics = "machine-status-events", groupId = "machine-bot-group")
    public void consume(MachineStatusChangeEvent event) {
        log.info(">>> Kafka получила событие: {}", event.machineId());
        machineEventService.process(event);
    }
}
