package com.example.demo;

import com.example.demo.entity.Subscriber;
import com.example.demo.repo.SubscriberRepository;
import com.example.demo.service.SubscriberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriberServiceExpandedTest {

    @Mock
    private SubscriberRepository repo;

    @InjectMocks
    private SubscriberService subscriberService;

    @Test
    void shouldSaveNewSubscriberWithTelegramLanguage() {
        when(repo.findByChatId(555L)).thenReturn(Optional.empty());
        ArgumentCaptor<Subscriber> captor = ArgumentCaptor.forClass(Subscriber.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subscriberService.register(555L, "vasya", "ru");

        verify(repo).save(captor.capture());
        assertEquals("ru", captor.getValue().getLanguage());
    }

    @Test
    void shouldDefaultToEnWhenLanguageCodeIsNull() {
        when(repo.findByChatId(556L)).thenReturn(Optional.empty());
        ArgumentCaptor<Subscriber> captor = ArgumentCaptor.forClass(Subscriber.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subscriberService.register(556L, "user", null);

        verify(repo).save(captor.capture());
        assertEquals("en", captor.getValue().getLanguage());
    }

    @Test
    void shouldSaveNewSubscriberWithChatIdAndUsername() {
        when(repo.findByChatId(777L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Subscriber result = subscriberService.register(777L, "testuser", "en");

        assertNotNull(result);
        assertEquals(777L, result.getChatId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void shouldReturnExistingSubscriberOnReRegister() {
        Subscriber existing = new Subscriber(1L, 123L, "andrey", LocalDateTime.now());
        existing.setLanguage("ru");
        when(repo.findByChatId(123L)).thenReturn(Optional.of(existing));

        Subscriber result = subscriberService.register(123L, "new_name", "en");

        assertSame(existing, result);
        verify(repo, never()).save(any());
        // язык не должен поменяться
        assertEquals("ru", result.getLanguage());
    }


    @ParameterizedTest
    @ValueSource(strings = {"ru", "en", "nl"})
    void shouldUpdateLanguageForExistingSubscriber(String newLang) {
        Subscriber sub = new Subscriber(1L, 100L, "user", LocalDateTime.now());
        sub.setLanguage("en");
        when(repo.findByChatId(100L)).thenReturn(Optional.of(sub));

        subscriberService.updateLanguage(100L, newLang);

        assertEquals(newLang, sub.getLanguage());
        verify(repo).save(sub);
    }

    @Test
    void shouldSetEnWhenUpdateLanguageCalledWithNull() {
        Subscriber sub = new Subscriber(1L, 100L, "user", LocalDateTime.now());
        sub.setLanguage("ru");
        when(repo.findByChatId(100L)).thenReturn(Optional.of(sub));

        subscriberService.updateLanguage(100L, null);

        assertEquals("en", sub.getLanguage());
        verify(repo).save(sub);
    }

    @Test
    void shouldDoNothingOnUpdateLanguageWhenSubscriberNotFound() {
        when(repo.findByChatId(999L)).thenReturn(Optional.empty());

        subscriberService.updateLanguage(999L, "ru");

        verify(repo, never()).save(any());
    }

    @Test
    void shouldPersistEmailCorrectly() {
        Subscriber sub = new Subscriber(1L, 100L, "user", LocalDateTime.now());
        when(repo.findByChatId(100L)).thenReturn(Optional.of(sub));

        subscriberService.setEmail(100L, "new@email.com");

        verify(repo).save(sub);
        assertEquals("new@email.com", sub.getEmail());
    }

    @Test
    void shouldOverwritePreviousEmail() {
        Subscriber sub = new Subscriber(1L, 100L, "user", LocalDateTime.now());
        sub.setEmail("old@email.com");
        when(repo.findByChatId(100L)).thenReturn(Optional.of(sub));

        subscriberService.setEmail(100L, "new@email.com");

        assertEquals("new@email.com", sub.getEmail());
    }

    @Test
    void existsShouldReturnFalseForUnknownChatId() {
        when(repo.findByChatId(12345L)).thenReturn(Optional.empty());
        assertFalse(subscriberService.exists(12345L));
    }

    @Test
    void existsShouldReturnTrueForRegisteredSubscriber() {
        Subscriber sub = new Subscriber(1L, 123L, "user", LocalDateTime.now());
        when(repo.findByChatId(123L)).thenReturn(Optional.of(sub));
        assertTrue(subscriberService.exists(123L));
    }
}