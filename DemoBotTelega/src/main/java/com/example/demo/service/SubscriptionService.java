package com.example.demo.service;

import com.example.demo.entity.MachineSubscription;
import com.example.demo.repo.MachineSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void unsubscribe(Long subscriberId, String machineId) {
        repo.deleteBySubscriberIdAndMachineId(subscriberId, machineId);
    }

    public List<String> listMachines(Long subscriberId) {
        return repo.findBySubscriberId(subscriberId)
                .stream()
                .map(MachineSubscription::getMachineId)
                .toList();
    }
}
