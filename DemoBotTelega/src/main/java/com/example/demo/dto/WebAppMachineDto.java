package com.example.demo.dto;

public class WebAppMachineDto {
    private String machineId;
    private boolean subscribed;

    public WebAppMachineDto(String machineId, boolean subscribed) {
        this.machineId = machineId;
        this.subscribed = subscribed;
    }

    public String getMachineId() {
        return machineId;
    }

    public boolean isSubscribed() {
        return subscribed;
    }
}
