package com.example.demo;

import com.example.demo.entity.Subscriber;
import com.example.demo.repo.SubscriberRepository;
import com.example.demo.service.SubscriberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriberServiceTest {

    @Mock
    private SubscriberRepository repo;

    @InjectMocks
    private SubscriberService subscriberService;

    // register возвращает существующего подписчика, не создавая нового
    @Test
    void shouldReturnExistingSubscriber() {
        Subscriber existing = new Subscriber(1L, 123L, "andrey", LocalDateTime.now());
        when(repo.findByChatId(123L)).thenReturn(Optional.of(existing));

        Subscriber result = subscriberService.register(123L, "new_username", "ru");

        assertEquals(1L, result.getId());
        assertEquals("andrey", result.getUsername());
        verify(repo, never()).save(any());
    }

    // register создаёт нового подписчика, если его нет в БД
    @Test
    void shouldRegisterNewSubscriber() {
        Subscriber saved = new Subscriber(10L, 555L, "vasya", LocalDateTime.now());
        when(repo.findByChatId(555L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenReturn(saved);

        Subscriber result = subscriberService.register(555L, "vasya", "ru");
        assertNotNull(result);
        assertEquals(555L, result.getChatId());
        verify(repo).save(any(Subscriber.class));
    }

    // getByChatId возвращает подписчика по chatId
    @Test
    void shouldGetSubscriberByChatId() {
        Subscriber sub = new Subscriber(1L, 123L, "andrey", LocalDateTime.now());
        when(repo.findByChatId(123L)).thenReturn(Optional.of(sub));

        Subscriber result = subscriberService.getByChatId(123L);

        assertNotNull(result);
        assertEquals(123L, result.getChatId());
    }

    // getByChatId возвращает null, если подписчик не найден
    @Test
    void shouldReturnNullWhenSubscriberNotFound() {
        when(repo.findByChatId(999L)).thenReturn(Optional.empty());

        Subscriber result = subscriberService.getByChatId(999L);

        assertNull(result);
    }

    // exists возвращает true для зарегистрированного chatId
    @Test
    void shouldReturnTrueIfSubscriberExists() {
        Subscriber sub = new Subscriber(1L, 123L, "andrey", LocalDateTime.now());
        when(repo.findByChatId(123L)).thenReturn(Optional.of(sub));

        assertTrue(subscriberService.exists(123L));
    }

    // exists возвращает false для незарегистрированного chatId
    @Test
    void shouldReturnFalseIfSubscriberDoesNotExist() {
        when(repo.findByChatId(999L)).thenReturn(Optional.empty());

        assertFalse(subscriberService.exists(999L));
    }

    // setEmail сохраняет email подписчика
    @Test
    void shouldSetEmailForSubscriber() {
        Subscriber sub = new Subscriber(1L, 123L, "andrey", LocalDateTime.now());
        when(repo.findByChatId(123L)).thenReturn(Optional.of(sub));

        subscriberService.setEmail(123L, "andrey@example.com");

        assertEquals("andrey@example.com", sub.getEmail());
        verify(repo).save(sub);
    }

    // setEmail ничего не делает, если подписчик не найден
    @Test
    void shouldDoNothingOnSetEmailIfSubscriberNotFound() {
        when(repo.findByChatId(999L)).thenReturn(Optional.empty());

        subscriberService.setEmail(999L, "nobody@example.com");

        verify(repo, never()).save(any());
    }
}