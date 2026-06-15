package com.example.demo;

import com.example.demo.bot.MachineBot;
import com.example.demo.entity.MachineSubscription;
import com.example.demo.entity.Subscriber;
import com.example.demo.machine.MachineStatus;
import com.example.demo.machine.MachineStatusChangeEvent;
import com.example.demo.repo.MachineSubscriptionRepository;
import com.example.demo.repo.SubscriberRepository;
import com.example.demo.service.EmailNotificationService;
import com.example.demo.service.MachineEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MachineEventServiceTest {

    @Mock
    private MachineSubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriberRepository subscriberRepository;

    @Mock
    private MachineBot machineBot;

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private MachineEventService machineEventService;




    @Test
    void shouldSendEmailToEachSubscriber() {
        MachineSubscription sub = new MachineSubscription(1L, 10L, "MACHINE-001", LocalDateTime.now());
        Subscriber subscriber = new Subscriber(10L, 1001L, "user1", LocalDateTime.now());

        MachineStatusChangeEvent event = new MachineStatusChangeEvent(
                "MACHINE-001", Instant.now(), MachineStatus.STOP, MachineStatus.RUN
        );

        when(subscriptionRepository.findByMachineId("MACHINE-001")).thenReturn(List.of(sub));
        when(subscriberRepository.findById(10L)).thenReturn(Optional.of(subscriber));

        machineEventService.process(event);
        verify(emailNotificationService).sendNotification(subscriber, event);
    }


}
