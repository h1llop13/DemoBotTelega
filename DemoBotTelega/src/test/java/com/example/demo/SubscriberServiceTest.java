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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriberServiceTest {

    @Mock
    private SubscriberRepository repo;

    @InjectMocks
    private SubscriberService subscriberService;


    // если пользователь зареган в БД -> новый не создастся
    @Test
    void shouldReturnExistingSubscriber() {

        Subscriber existing =
                new Subscriber(
                        1L,
                        123L,
                        "andrey",
                        LocalDateTime.now()
                );

        when(repo.findByChatId(123L))
                .thenReturn(Optional.of(existing));

        Subscriber result =
                subscriberService.register(123L, "new_username");

        assertEquals(1L, result.getId());
        assertEquals("andrey", result.getUsername());

        verify(repo, never()).save(any());
    }
}