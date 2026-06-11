package com.example.demo.repo;

import com.example.demo.entity.MachineSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineSubscriptionRepository extends JpaRepository<MachineSubscription, Long> {

    List<MachineSubscription> findBySubscriberId(Long subscriberId);

    List<MachineSubscription> findByMachineId(String machineId);

    boolean existsBySubscriberIdAndMachineId(Long subscriberId, String machineId);

    void deleteBySubscriberIdAndMachineId(Long subscriberId, String machineId);
}
