package com.example.demo.handler;

import com.example.demo.bot.MachineBot;

public class MenuListHandler implements CallbackHandler {
    private final MachineBot bot;
    public MenuListHandler(MachineBot bot) { this.bot = bot; }
    @Override
    public void handle(Long chatId, String data) {
        bot.showSubscriptions(chatId);
    }
}
