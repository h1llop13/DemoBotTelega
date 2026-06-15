package com.example.demo.service;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LocaleService {

    private final MessageSource messageSource;
    private final SubscriberService subscriberService;

    public LocaleService(MessageSource messageSource, SubscriberService subscriberService) {
        this.messageSource = messageSource;
        this.subscriberService = subscriberService;
    }

    // Получить перевод по chatId пользователя
    public String msg(Long chatId, String key, Object... args) {
        Locale locale = getLocale(chatId);
        return messageSource.getMessage(key, args, locale);
    }

    // Получить перевод по явно заданному language code ("ru", "en", "nl")
    public String msg(String language, String key, Object... args) {
        Locale locale = toLocale(language);
        return messageSource.getMessage(key, args, locale);
    }

    // Определить Locale пользователя из БД
    public Locale getLocale(Long chatId) {
        var subscriber = subscriberService.getByChatId(chatId);
        if (subscriber == null || subscriber.getLanguage() == null) {
            return Locale.ENGLISH;
        }
        return toLocale(subscriber.getLanguage());
    }

    private Locale toLocale(String lang) {
        if (lang == null) return Locale.ENGLISH;
        return switch (lang) {
            case "ru" -> new Locale("ru");
            case "nl" -> new Locale("nl");
            default  -> Locale.ENGLISH;
        };
    }
}