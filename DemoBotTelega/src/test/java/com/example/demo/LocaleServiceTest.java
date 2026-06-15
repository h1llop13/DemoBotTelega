package com.example.demo;

import com.example.demo.entity.Subscriber;
import com.example.demo.service.LocaleService;
import com.example.demo.service.SubscriberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LocaleServiceTest {

    @Mock
    private MessageSource messageSource;

    @Mock
    private SubscriberService subscriberService;

    @InjectMocks
    private LocaleService localeService;

    @Test
    void shouldUseSubscriberLanguageForChatId() {
        Subscriber sub = new Subscriber(1L, 10L, "user", LocalDateTime.now());
        sub.setLanguage("ru");

        when(subscriberService.getByChatId(100L)).thenReturn(sub);
        when(messageSource.getMessage(eq("bot.welcome"), isNull(), eq(new Locale("ru")))).thenReturn("Добро пожаловать");

        String result = localeService.msg(100L, "bot.welcome");
        assertEquals("Добро пожаловать", result);
    }

    @Test
    void shouldUseEnglishLocaleForEnSubscriber() {
        Subscriber sub = new Subscriber(1L, 200L, "user", LocalDateTime.now());
        sub.setLanguage("en");

        when(subscriberService.getByChatId(200L)).thenReturn(sub);
        when(messageSource.getMessage(eq("bot.welcome"), isNull(), eq(Locale.ENGLISH)))
                .thenReturn("Welcome");

        String result = localeService.msg(300L,  "bot.welcome");
        assertEquals("Welkom", result);
    }

    @Test
    void shouldUseDutchLocaleForNlSubscriber() {
        Subscriber sub = new Subscriber(1L, 300L, "user", LocalDateTime.now());
        sub.setLanguage(null);

        when(subscriberService.getByChatId(300L)).thenReturn(sub);
        when(messageSource.getMessage(eq("bot.welcome"), isNull(), eq(Locale.ENGLISH)))
                .thenReturn("Welcome");

        String result = localeService.msg(300L, "bot.welcome");
        assertEquals("Welkom", result);
    }

    @Test
    void shouldFallbackToEnglishWhenLanguageIsNull() {
        Subscriber sub = new Subscriber(1L, 400L, "user", LocalDateTime.now());
        sub.setLanguage(null);

        when(subscriberService.getByChatId(400L)).thenReturn(sub);
        when(messageSource.getMessage(eq("bot.welcome"), isNull(), eq(Locale.ENGLISH)))
                .thenReturn("Welcome");

        String result = localeService.msg(400L, "bot.welcome");
        assertEquals("Welkom", result);
    }

    @Test
    void shouldFallbackToEnglishWhenSubscriberIsNull() {
        when(subscriberService.getByChatId(999L)).thenReturn(null);
        when(messageSource.getMessage(eq("bot.welcome"), isNull(), eq(Locale.ENGLISH))).thenReturn("Welcome");

        String result = localeService.msg(999L, "bot.welcome");
        assertEquals("Welcome", result);
    }


    @ParameterizedTest
    @CsvSource({"ru,Добро пожаловать", "en,Welcome", "nl,Welkom"})
    void shouldReturnMessageByLanguageCode(String lang, String expected) {
        Locale expectedLocale = switch (lang) {
            case "ru" -> new Locale("ru");
            case "nl" -> new Locale("nl");
            default -> Locale.ENGLISH;
        };
        when(messageSource.getMessage(eq("bot.welcome"), isNull(), eq(expectedLocale))).thenReturn(expected);

        String result = localeService.msg(lang, "bot.welcome");
        assertEquals(expected, result);

        verifyNoInteractions(subscriberService);
    }

    @Test
    void shouldPassArgsToMessageSource() {
        Subscriber sub = new Subscriber(1L, 100L, "user", LocalDateTime.now());
        sub.setLanguage("en");

        when(subscriberService.getByChatId(100L)).thenReturn(sub);
        when(messageSource.getMessage(
                eq("bot.machine_added"), eq(new Object[]{"MACHINE-001"}), eq(Locale.ENGLISH))).thenReturn("Machine MACHINE-001 added");

        String result = localeService.msg(100L, "bot.machine_added", "MACHINE-001");
        assertEquals("Machine MACHINE-001 added", result);
    }

    @Test
    void shouldReturnEnglishLocaleForUnknownLanguageCode() {
        Subscriber sub = new Subscriber(1L, 100L, "user", LocalDateTime.now());
        sub.setLanguage("fr");

        when(subscriberService.getByChatId(100L)).thenReturn(sub);

        Locale locale = localeService.getLocale(100L);
        assertEquals(Locale.ENGLISH, locale);
    }
}
