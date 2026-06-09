package com.example.demo.handler;

import com.example.demo.bot.MachineBot;

public class MenuSubscribeHandler implements CallbackHandler {
    private final MachineBot bot;
    public MenuSubscribeHandler(MachineBot bot) { this.bot = bot; }
    @Override
    public void handle(Long chatId, String data) {
        bot.showMachines(chatId);
    }
}
