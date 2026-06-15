package com.example.demo.service;

import com.example.demo.entity.Subscriber;
import com.example.demo.repo.SubscriberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SubscriberService {

    private final SubscriberRepository repo;

    public SubscriberService(SubscriberRepository repo) {
        this.repo = repo;
    }

    // languageCode — код языка из Telegram ("ru", "en", "nl" и т.д.)
    public Subscriber register(Long chatId, String username, String languageCode) {
        return repo.findByChatId(chatId)
                .orElseGet(() -> {
                    Subscriber s = new Subscriber(null, chatId, username, LocalDateTime.now());
                    s.setLanguage(languageCode != null ? languageCode : "en");
                    return repo.save(s);
                });
    }

    public Subscriber getByChatId(Long chatId) {
        return repo.findByChatId(chatId).orElse(null);
    }

    public boolean exists(Long chatId) {
        return repo.findByChatId(chatId).isPresent();
    }

    public void setEmail(Long chatId, String email) {
        repo.findByChatId(chatId).ifPresent(subscriber -> {
            subscriber.setEmail(email);
            repo.save(subscriber);
        });
    }

    @Transactional
    public void updateLanguage(Long chatId, String newLanguageCode) {
        repo.findByChatId(chatId).ifPresent(subscriber -> {
            subscriber.setLanguage(newLanguageCode != null ? newLanguageCode : "en");
            repo.save(subscriber);
        });
    }
}