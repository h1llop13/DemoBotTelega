package com.example.demo.controller;

import com.example.demo.machine.MachineStatusChangeEvent;
import com.example.demo.service.MachineEventProducer;
import com.example.demo.service.MachineEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/machines")
public class MachineController {

    private final MachineEventProducer machineEventProducer; // <-- было MachineEventService

    public MachineController(MachineEventProducer machineEventProducer) {
        this.machineEventProducer = machineEventProducer;
    }

    @PostMapping("status")
    public ResponseEntity<?> statusChanged(@RequestBody MachineStatusChangeEvent event) {
        machineEventProducer.publish(event); // кладём в Kafka и сразу отвечаем
        return ResponseEntity.ok(Map.of("status", "accepted"));
    }
}
