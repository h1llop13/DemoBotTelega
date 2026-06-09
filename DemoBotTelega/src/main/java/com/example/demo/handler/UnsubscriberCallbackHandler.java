package com.example.demo.handler;

import com.example.demo.bot.MachineBot;
import com.example.demo.service.SubscriberService;
import com.example.demo.service.SubscriptionService;

public class UnsubscriberCallbackHandler implements CallbackHandler {
    private final SubscriberService subscriberService;
    private final SubscriptionService subscriptionService;
    private final MachineBot bot;
    public UnsubscriberCallbackHandler(SubscriberService subscriberService,
                                       SubscriptionService subscriptionService,
                                       MachineBot bot) {
        this.subscriberService = subscriberService;
        this.subscriptionService = subscriptionService;
        this.bot = bot;
    }
    @Override
    public void handle(Long chatId, String data) {
        String machineId = data.replace("UNSUB_", "");
        var sub = subscriberService.getByChatId(chatId);
        subscriptionService.unsubscribe(sub.getId(), machineId);
        bot.send(chatId, "❌ Отписка: " + machineId);
    }
}
