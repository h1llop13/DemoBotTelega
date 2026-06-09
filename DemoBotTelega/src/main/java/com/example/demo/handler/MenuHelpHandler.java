package com.example.demo.handler;

import com.example.demo.bot.MachineBot;

public class MenuHelpHandler implements CallbackHandler {
    private final MachineBot bot;
    public MenuHelpHandler(MachineBot bot) { this.bot = bot; }
    @Override
    public void handle(Long chatId, String data) {
        bot.send(chatId, "/start\n/help\n/subscribe\n/unsubscribe\n/list");
    }
}
