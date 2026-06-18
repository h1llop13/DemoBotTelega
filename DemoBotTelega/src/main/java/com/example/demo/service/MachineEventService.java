package com.example.demo.service;

import com.example.demo.bot.MachineBot;
import com.example.demo.machine.MachineStatusChangeEvent;
import com.example.demo.repo.SubscriberRepository;
import com.example.demo.repo.MachineSubscriptionRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MachineEventService {

    private final MachineSubscriptionRepository subscriptionRepository;
    private final SubscriberRepository subscriberRepository;
    private final MachineBot machineBot;
    private final EmailNotificationService emailNotificationService;
    private static final Logger log = LoggerFactory.getLogger(MachineEventService.class);

    public MachineEventService(
            MachineSubscriptionRepository subscriptionRepository,
            SubscriberRepository subscriberRepository,
            @Lazy MachineBot machineBot,
            EmailNotificationService emailNotificationService) {

        this.subscriptionRepository = subscriptionRepository;
        this.subscriberRepository = subscriberRepository;
        this.machineBot = machineBot;
        this.emailNotificationService = emailNotificationService;
    }

    public void process(MachineStatusChangeEvent event) {
        var subscriptions = subscriptionRepository.findByMachineId(event.machineId());
        String text = buildMessage(event);
        InlineKeyboardMarkup keyboard = buildUnsubscribeButton(event.machineId());

        for (var subscription : subscriptions) {
            subscriberRepository.findById(subscription.getSubscriberId()).ifPresent(s -> {
                try {
                    machineBot.sendWithKeyboard(s.getChatId(), text, keyboard);
                } catch (Exception e) {
                    log.error("Не удалось отправить Telegram-уведомление подписчику {}: {}", s.getChatId(), e.getMessage());
                }

                try {
                    emailNotificationService.sendNotification(s, event);
                } catch (Exception e) {
                    log.error("Не удалось отправить email-уведомление подписчику {}: {}", s.getChatId(), e.getMessage());
                }
            });
        }
    }

    private InlineKeyboardMarkup buildUnsubscribeButton(String machineId) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Отписаться от этого станка");
        button.setCallbackData("UNSUB_" + machineId);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(button)));
        return markup;
    }

    private String buildMessage(MachineStatusChangeEvent event) {
        String time = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                .withZone(ZoneOffset.UTC)
                .format(event.stamp());

        return """
                ⚙ Станок: %s

                Изменение статуса:

                %s → %s

                Время:
                %s UTC
                """
                .formatted(event.machineId(), event.prevState(), event.newState(), time);
    }
}
