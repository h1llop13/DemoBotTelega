package com.example.demo.handler;

import com.example.demo.bot.MachineBot;

public class InfoCallbackHandler implements CallbackHandler {
    private final MachineBot bot;
    public InfoCallbackHandler(MachineBot bot) { this.bot = bot; }
    @Override
    public void handle(Long chatId, String data) {
        String machineId = data.replace("INFO_", "");
        bot.send(chatId, "Станок: " + machineId);
    }
}