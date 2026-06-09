package com.example.demo.service;

import com.example.demo.entity.Machine;
import com.example.demo.repo.MachineRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MachineService {
    private final MachineRepository machineRepository;

    public MachineService(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    public List<String> getAllMachineIds() {
        return machineRepository.findAll()
                .stream()
                .map(Machine::getMachineId)
                .toList();
    }

    public void addMachine(String machineId) {
        if (!machineRepository.existsByMachineId(machineId)) {
            machineRepository.save(new Machine(machineId));
        }
    }

    public boolean deleteMachine(String machineId) {
        return machineRepository.findByMachineId(machineId)
                .map(machine -> {
                    machineRepository.delete(machine);
                    return true;
                })
                .orElse(false);
    }
}
