package com.example.demo.handler;

import com.example.demo.bot.MachineBot;
import com.example.demo.service.SubscriberService;
import com.example.demo.service.SubscriptionService;

public class SubscribeCallbackHandler implements CallbackHandler {
    private final SubscriberService subscriberService;
    private final SubscriptionService subscriptionService;
    private final MachineBot bot;
    public SubscribeCallbackHandler(SubscriberService subscriberService,
                                    SubscriptionService subscriptionService,
                                    MachineBot bot) {
        this.subscriberService = subscriberService;
        this.subscriptionService = subscriptionService;
        this.bot = bot;
    }
    @Override
    public void handle(Long chatId, String data) {
        String machineId = data.replace("SUB_", "");
        var sub = subscriberService.getByChatId(chatId);
        subscriptionService.subscribe(sub.getId(), machineId);
        bot.send(chatId, "📡 Подписка: " + machineId);
    }
}