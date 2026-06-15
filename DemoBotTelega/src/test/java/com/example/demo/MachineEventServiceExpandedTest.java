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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MachineEventServiceExpandedTest {

    @Mock private MachineSubscriptionRepository subscriptionRepository;
    @Mock private SubscriberRepository subscriberRepository;
    @Mock private MachineBot machineBot;
    @Mock private EmailNotificationService emailNotificationService;

    @InjectMocks
    private MachineEventService machineEventService;

    private MachineStatusChangeEvent event(String machineId) {
        return new MachineStatusChangeEvent(machineId, Instant.now(),
                MachineStatus.STOP, MachineStatus.RUN);
    }

    private Subscriber subscriber(Long id, Long chatId) {
        return new Subscriber(id, chatId, "user", LocalDateTime.now());
    }

    private MachineSubscription subscription(Long subId, String machineId) {
        return new MachineSubscription(1L, subId, machineId, LocalDateTime.now());
    }

    @Test
    void shouldSendTelegramMessageToAllSubscribers() {
        MachineSubscription sub1 = subscription(10L, "MACHINE-001");
        MachineSubscription sub2 = subscription(20L, "MACHINE-001");
        Subscriber s1 = subscriber(10L, 1001L);
        Subscriber s2 = subscriber(20L, 1002L);

        when(subscriptionRepository.findByMachineId("MACHINE-001")).thenReturn(List.of(sub1, sub2));
        when(subscriberRepository.findById(10L)).thenReturn(Optional.of(s1));
        when(subscriberRepository.findById(20L)).thenReturn(Optional.of(s2));

        machineEventService.process(event("MACHINE-001"));

        verify(machineBot, times(2)).sendWithKeyboard(anyLong(), anyString(), any(InlineKeyboardMarkup.class));
    }

    @Test
    void shouldSendEmailToEachSubscriber() {
        MachineSubscription sub = subscription(10L, "MACHINE-001");
        Subscriber s = subscriber(10L, 1001L);

        when(subscriptionRepository.findByMachineId("MACHINE-011")).thenReturn(List.of(sub));
        when(subscriberRepository.findById(10L)).thenReturn(Optional.of(s));
        machineEventService.process(event("MACHINE-001"));
        verify(emailNotificationService).sendNotification(s, event("MACHINE-001"));
    }

    @Test
    void shouldNotSendAnythingWhenNoSubscriptions() {
        when(subscriptionRepository.findByMachineId("MACHINE-777")).thenReturn(List.of());

        machineEventService.process(event("MACHINE-777"));

        verify(machineBot, never()).sendWithKeyboard(anyLong(), anyString(), any(InlineKeyboardMarkup.class));
        verify(emailNotificationService, never()).sendNotification(any(), any());
    }

    @Test
    void shouldSkipWhenSubscriberNotFoundInRepository() {
        MachineSubscription sub = subscription(99L, "MACHINE-001");
        when(subscriptionRepository.findByMachineId("MACHINE-001")).thenReturn(List.of(sub));
        when(subscriberRepository.findById(99L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> machineEventService.process(event("MACHINE-001")));
        verify(machineBot, never()).sendWithKeyboard(anyLong(), anyString(), any(InlineKeyboardMarkup.class));
    }

    @Test
    void shouldIncludeMachineIdInTelegramMessage() {
        MachineSubscription sub = subscription(10L, "MACHINE-XYZ");
        Subscriber s = subscriber(10L, 1001L);

        when(subscriptionRepository.findByMachineId("MACHINE-XYZ")).thenReturn(List.of(sub));
        when(subscriberRepository.findById(10L)).thenReturn(Optional.of(s));

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        machineEventService.process(event("MACHINE-XYZ"));

        verify(machineBot).sendWithKeyboard(eq(1001L), textCaptor.capture(), any(InlineKeyboardMarkup.class));
        assertTrue(textCaptor.getValue().contains("MACHINE-XYZ"));
    }

    @Test
    void shouldIncludeStatusTransitionInTelegramMessage() {
        MachineStatusChangeEvent evt = new MachineStatusChangeEvent(
                "MACHINE-001", Instant.now(), MachineStatus.RUN, MachineStatus.STOP);
        MachineSubscription sub = subscription(10L, "MACHINE-001");
        Subscriber s = subscriber(10L, 1001L);

        when(subscriptionRepository.findByMachineId("MACHINE-001")).thenReturn(List.of(sub));
        when(subscriberRepository.findById(10L)).thenReturn(Optional.of(s));

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        machineEventService.process(evt);

        verify(machineBot).sendWithKeyboard(eq(1001L), textCaptor.capture(), any(InlineKeyboardMarkup.class));
        String msg = textCaptor.getValue();
        assertTrue(msg.contains("RUN"));
        assertTrue(msg.contains("STOP"));
    }

    @Test
    void shouldIncludeUnsubscribeButtonInKeyboard() {
        MachineSubscription sub = subscription(10L, "MACHINE-001");
        Subscriber s = subscriber(10L, 1001L);

        when(subscriptionRepository.findByMachineId("MACHINE-001")).thenReturn(List.of(sub));
        when(subscriberRepository.findById(10L)).thenReturn(Optional.of(s));

        ArgumentCaptor<InlineKeyboardMarkup> keyboardCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);
        machineEventService.process(event("MACHINE-001"));

        verify(machineBot).sendWithKeyboard(anyLong(), anyString(), keyboardCaptor.capture());
        InlineKeyboardMarkup markup = keyboardCaptor.getValue();
        boolean hasUnsubButton = markup.getKeyboard().stream()
                .flatMap(List::stream)
                .anyMatch(btn -> btn.getCallbackData().startsWith("UNSUB_"));
        assertTrue(hasUnsubButton);
    }

    @Test
    void shouldHandleMultipleDifferentMachinesIndependently() {
        MachineSubscription sub1 = subscription(10L, "MACHINE-A");
        MachineSubscription sub2 = subscription(20L, "MACHINE-B");
        Subscriber s1 = subscriber(10L, 1001L);
        Subscriber s2 = subscriber(20L, 1002L);

        when(subscriptionRepository.findByMachineId("MACHINE-A")).thenReturn(List.of(sub1));
        when(subscriptionRepository.findByMachineId("MACHINE-B")).thenReturn(List.of(sub2));
        when(subscriberRepository.findById(10L)).thenReturn(Optional.of(s1));
        when(subscriberRepository.findById(20L)).thenReturn(Optional.of(s2));

        machineEventService.process(event("MACHINE-A"));
        machineEventService.process(event("MACHINE-B"));

        verify(machineBot).sendWithKeyboard(eq(1001L), anyString(), any(InlineKeyboardMarkup.class));
        verify(machineBot).sendWithKeyboard(eq(1002L), anyString(), any(InlineKeyboardMarkup.class));
    }
}
