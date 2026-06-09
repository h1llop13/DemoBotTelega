package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String machineId;

    public Machine() {}

    public Machine(String machineId) {
        this.machineId = machineId;
    }

    public Long getId() {
        return id;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }
}
