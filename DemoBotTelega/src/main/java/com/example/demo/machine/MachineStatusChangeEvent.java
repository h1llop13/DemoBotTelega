package com.example.demo.machine;

import java.time.Instant;

public record MachineStatusChangeEvent(
        String machineId,
        Instant stamp,
        MachineStatus prevState,
        MachineStatus newState
) {
}
