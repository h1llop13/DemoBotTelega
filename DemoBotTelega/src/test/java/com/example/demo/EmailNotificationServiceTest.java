package com.example.demo;

import com.example.demo.entity.Subscriber;
import com.example.demo.machine.MachineStatus;
import com.example.demo.machine.MachineStatusChangeEvent;
import com.example.demo.service.EmailNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    private Subscriber subscriberWithEmail(String email) {
        Subscriber s = new Subscriber(1L, 100L, "user", LocalDateTime.now());
        s.setEmail(email);
        return  s;
    }

    private MachineStatusChangeEvent sampleEvent() {
        return new MachineStatusChangeEvent(
                "MACHINE-001",
                Instant.parse("2025-01-01T10:00:00Z"),
                MachineStatus.STOP,
                MachineStatus.RUN
        );
    }

    @Test
    void shouldSendEmailWhenSubscriberHasEmail() {
        Subscriber sub = subscriberWithEmail("test@example.com");
        emailNotificationService.sendNotification(sub, sampleEvent());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSetCorrectRecipientAddress() {
        Subscriber sub = subscriberWithEmail("user@factory.com");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailNotificationService.sendNotification(sub, sampleEvent());

        verify(mailSender).send(captor.capture());
        assertArrayEquals(new String[]{"user@factory.com"}, captor.getValue().getTo());
    }

    @Test
    void shouldIncludeMachineIdInSubject() {
        Subscriber sub = subscriberWithEmail("user@factory.com");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailNotificationService.sendNotification(sub, sampleEvent());

        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("MACHINE-001"));
    }

    @Test
    void shouldIncludeStatusTransitionInBody() {
        Subscriber sub = subscriberWithEmail("user@factory.com");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailNotificationService.sendNotification(sub, sampleEvent());

        verify(mailSender).send(captor.capture());
        String body = captor.getValue().getText();
        assertNotNull(body);
        assertTrue(body.contains("STOP"));
        assertTrue(body.contains("RUN"));
    }

    @Test
    void shouldIncludeTimestampInBody() {
        Subscriber sub = subscriberWithEmail("user@factory.com");
        MachineStatusChangeEvent event = new MachineStatusChangeEvent(
                "MACHINE-001",
                Instant.parse("2025-06-15T12:30:00Z"),
                MachineStatus.RUN,
                MachineStatus.STOP
        );
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailNotificationService.sendNotification(sub, event);

        verify(mailSender).send(captor.capture());
        // timestamp должен присутствовать в теле (в любом формате)
        assertTrue(captor.getValue().getText().contains("2025"));
    }

    @Test
    void shouldNotSendEmailWhenEmailIsBlank() {
        Subscriber sub = subscriberWithEmail("   ");
        emailNotificationService.sendNotification(sub, sampleEvent());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldNotSendEmailWhenEmailIsNull() {
        Subscriber sub = subscriberWithEmail(null);
        emailNotificationService.sendNotification(sub, sampleEvent());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldNotSendEmailWhenEmailIsEmpty() {
        Subscriber sub = subscriberWithEmail("");
        emailNotificationService.sendNotification(sub, sampleEvent());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
