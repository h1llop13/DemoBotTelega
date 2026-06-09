package com.example.demo.controller;

import com.example.demo.machine.MachineStatusChangeEvent;
import com.example.demo.service.MachineEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/machines")
public class MachineController {
    private final MachineEventService machineEventService;

    public MachineController(MachineEventService machineEventService) {
        this.machineEventService = machineEventService;
    }

    @PostMapping("status")
    public ResponseEntity<?> statusChanged(@RequestBody MachineStatusChangeEvent event) {

        machineEventService.process(event);

        return ResponseEntity.ok(
                Map.of("status", "accepted")
        );
    }
}
