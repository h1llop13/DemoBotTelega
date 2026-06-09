package com.example.demo.service;

import com.example.demo.entity.Subscriber;
import com.example.demo.repo.SubscriberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SubscriberService {

    private final SubscriberRepository repo;

    public SubscriberService(SubscriberRepository repo) {
        this.repo = repo;
    }

    public Subscriber register(Long chatId, String username) {
        return repo.findByChatId(chatId)
                .orElseGet(() -> repo.save(
                        new Subscriber(null, chatId, username, LocalDateTime.now())
                ));
    }

    public Subscriber getByChatId(Long chatId) {
        return repo.findByChatId(chatId).orElse(null);
    }

    public boolean exists(Long  chatId) {
        return repo.findByChatId(chatId).isPresent();
    }
}






