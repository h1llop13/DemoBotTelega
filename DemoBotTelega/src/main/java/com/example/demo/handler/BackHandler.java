package com.example.demo.handler;

import com.example.demo.bot.MachineBot;

public class BackHandler implements CallbackHandler {
    private final MachineBot bot;

    public BackHandler(MachineBot bot) {
        this.bot = bot;
    }

    @Override
    public void handle(Long chatId, String data) {
        bot.sendMainMenu(chatId);
    }
}
