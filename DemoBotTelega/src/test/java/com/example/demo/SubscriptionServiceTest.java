package com.example.demo;

import com.example.demo.entity.MachineSubscription;
import com.example.demo.repo.MachineSubscriptionRepository;
import com.example.demo.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private MachineSubscriptionRepository repo;

    @InjectMocks
    private SubscriptionService service;

    // пользователь подписывается на станок и запись создается в БД, если ее нет
    @Test
    void shouldCreateSubscriptionIfNotExists() {

        Long subscriberId = 1L;
        String machineId = "MACHINE-001";


        when(repo.existsBySubscriberIdAndMachineId(subscriberId, machineId)).thenReturn(false);

        service.subscribe(subscriberId, machineId);

        verify(repo).save(any(MachineSubscription.class));
    }
}
