package com.example.demo.service;

import com.example.demo.entity.Subscriber;
import com.example.demo.machine.MachineStatusChangeEvent;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {
    private final JavaMailSender mailSender;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendNotification(Subscriber subscriber, MachineStatusChangeEvent event) {
        if (subscriber.getEmail() == null || subscriber.getEmail().isBlank()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(subscriber.getEmail());
        message.setSubject("Станок " + event.machineId() + " изменил статус");
        message.setText(
                "Станок: " + event.machineId() + "\n" +
                        "Статус: " + event.prevState() + " -> " + event.newState() + "\n" +
                        "Время: " + event.stamp()
        );
        mailSender.send(message);
    }
}
