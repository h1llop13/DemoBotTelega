package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class MachineSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long subscriberId;
    private String machineId;
    private LocalDateTime createdAt = LocalDateTime.now();

    public MachineSubscription() {}

    public MachineSubscription(Long id, Long subscriberId, String machineId, LocalDateTime createdAt) {
        this.id = id;
        this.subscriberId = subscriberId;
        this.machineId = machineId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getSubscriberId() {
        return subscriberId;
    }

    public String getMachineId() {
        return machineId;
    }
}
