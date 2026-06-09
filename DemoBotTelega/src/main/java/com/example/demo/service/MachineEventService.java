package com.example.demo.service;

import com.example.demo.bot.MachineBot;
import com.example.demo.machine.MachineStatusChangeEvent;
import com.example.demo.repo.SubscriberRepository;
import com.example.demo.repo.MachineSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.List;

@Service
public class MachineEventService {

    private final MachineSubscriptionRepository subscriptionRepository;
    private final SubscriberRepository subscriberRepository;
    private final MachineBot machineBot;

    public MachineEventService(
            MachineSubscriptionRepository subscriptionRepository,
            SubscriberRepository subscriberRepository,
            MachineBot machineBot) {

        this.subscriptionRepository = subscriptionRepository;
        this.subscriberRepository = subscriberRepository;
        this.machineBot = machineBot;
    }

    public void process(MachineStatusChangeEvent event) {
        var subscriptions = subscriptionRepository.findByMachineId(event.machineId());
        String text = buildMessage(event);
        InlineKeyboardMarkup keyboard = buildUnsubscribeButton(event.machineId());

        for (var subscription : subscriptions) {
            var subscriber = subscriberRepository.findById(subscription.getSubscriberId());
            subscriber.ifPresent(s -> {
                machineBot.sendNotificationWithKeyboard(
                        subscriber.get().getChatId(),
                        text,
                        keyboard
                );
            });
        }
    }

    private InlineKeyboardMarkup buildUnsubscribeButton(String machineId) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Отписаться от этого станка");
        button.setCallbackData("UNSUB_" + machineId);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(button)));
        return  markup;
    }

    private String buildMessage(
            MachineStatusChangeEvent event) {

        String time =
                DateTimeFormatter.ofPattern(
                                "dd.MM.yyyy HH:mm:ss")
                        .withZone(ZoneOffset.UTC)
                        .format(event.stamp());

        return """
                ⚙ Станок: %s

                Изменение статуса:

                %s → %s

                Время:
                %s UTC
                """
                .formatted(
                        event.machineId(),
                        event.prevState(),
                        event.newState(),
                        time
                );
    }
}