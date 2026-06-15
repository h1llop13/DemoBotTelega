package com.example.demo;

import com.example.demo.entity.MachineSubscription;
import com.example.demo.repo.MachineSubscriptionRepository;
import com.example.demo.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private MachineSubscriptionRepository repo;

    @InjectMocks
    private SubscriptionService service;

    // subscribe создаёт запись, если подписки ещё не существует
    @Test
    void shouldCreateSubscriptionIfNotExists() {
        when(repo.existsBySubscriberIdAndMachineId(1L, "MACHINE-001")).thenReturn(false);

        service.subscribe(1L, "MACHINE-001");

        verify(repo).save(any(MachineSubscription.class));
    }

    // subscribe не создаёт дубликат, если подписка уже есть
    @Test
    void shouldNotCreateDuplicateSubscription() {
        when(repo.existsBySubscriberIdAndMachineId(1L, "MACHINE-001")).thenReturn(true);

        service.subscribe(1L, "MACHINE-001");

        verify(repo, never()).save(any());
    }

    // unsubscribe вызывает удаление записи из репозитория
    @Test
    void shouldCallDeleteOnUnsubscribe() {
        service.unsubscribe(1L, "MACHINE-001");

        verify(repo).deleteBySubscriberIdAndMachineId(1L, "MACHINE-001");
    }

    // listMachines возвращает список id станков для подписчика
    @Test
    void shouldReturnMachineListForSubscriber() {
        MachineSubscription sub1 = new MachineSubscription(1L, 10L, "MACHINE-001", LocalDateTime.now());
        MachineSubscription sub2 = new MachineSubscription(2L, 10L, "MACHINE-002", LocalDateTime.now());
        when(repo.findBySubscriberId(10L)).thenReturn(List.of(sub1, sub2));

        List<String> result = service.listMachines(10L);

        assertEquals(2, result.size());
        assertTrue(result.contains("MACHINE-001"));
        assertTrue(result.contains("MACHINE-002"));
    }

    // listMachines возвращает пустой список, если подписок нет
    @Test
    void shouldReturnEmptyListWhenNoSubscriptions() {
        when(repo.findBySubscriberId(99L)).thenReturn(List.of());

        List<String> result = service.listMachines(99L);

        assertTrue(result.isEmpty());
    }
}