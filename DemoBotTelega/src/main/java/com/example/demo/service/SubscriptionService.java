package com.example.demo.service;

import com.example.demo.entity.MachineSubscription;
import com.example.demo.repo.MachineSubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubscriptionService {

    private final MachineSubscriptionRepository repo;

    public SubscriptionService(MachineSubscriptionRepository repo) {
        this.repo = repo;
    }

    public void subscribe(Long subscriberId, String machineId) {
        if (repo.existsBySubscriberIdAndMachineId(subscriberId, machineId)) return;

        repo.save(new MachineSubscription(
                null,
                subscriberId,
                machineId,
                LocalDateTime.now()
        ));
    }

    public void unsubscribe(Long subscriberId, String machineId) {
        repo.findAll().stream()
                .filter(s -> s.getSubscriberId().equals(subscriberId)
                        && s.getMachineId().equals(machineId))
                .findFirst()
                .ifPresent(repo::delete);
    }

    public List<String> listMachines(Long subscriberId) {
        return repo.findBySubscriberId(subscriberId)
                .stream()
                .map(MachineSubscription::getMachineId)
                .toList();
    }
}
