package com.example.demo.service;

import com.example.demo.entity.Machine;
import com.example.demo.repo.MachineRepository;
import com.example.demo.repo.MachineSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MachineService {

    private final MachineRepository machineRepository;
    private final MachineSubscriptionRepository machineSubscriptionRepository;

    public MachineService(MachineRepository machineRepository,
                          SubscriptionService subscriptionService,
                          MachineSubscriptionRepository machineSubscriptionRepository) {
        this.machineRepository = machineRepository;
        this.machineSubscriptionRepository = machineSubscriptionRepository;
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

    @Transactional
    public boolean deleteMachine(String machineId) {
        return machineRepository.findByMachineId(machineId)
                .map(machine -> {
                    machineSubscriptionRepository.deleteByMachineId(machineId);
                    machineRepository.delete(machine);
                    return true;
                })
                .orElse(false);
    }
}
