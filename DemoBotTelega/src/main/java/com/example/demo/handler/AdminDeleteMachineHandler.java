package com.example.demo.handler;

import com.example.demo.bot.MachineBot;

public class AdminDeleteMachineHandler implements CallbackHandler {
    private final MachineBot bot;
    public AdminDeleteMachineHandler(MachineBot bot) { this.bot = bot; }
    @Override
    public void handle(Long chatId, String data) {
        if (!bot.isAdmin(chatId)) {
            bot.send(chatId, "Нет доступа!");
            return;
        }
        bot.getAdminState().put(chatId, "WAIT_DELETE_MACHINE_NAME");
        bot.send(chatId, "Введите название станка для удаления: \nMACHINE-XXX");
    }
}
