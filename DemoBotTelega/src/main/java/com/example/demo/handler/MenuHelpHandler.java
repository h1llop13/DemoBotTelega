package com.example.demo.handler;

import com.example.demo.bot.MachineBot;

public class MenuHelpHandler implements CallbackHandler {
    private final MachineBot bot;
    public MenuHelpHandler(MachineBot bot) { this.bot = bot; }
    @Override
    public void handle(Long chatId, String data) {
        bot.send(chatId, "/start - Перезапуск\n" +
                "/subscribe <...> - Подписаться\n" +
                "/unsubscribe <...> - Отписаться\n" +
                "/list - Список подписок\n" +
                "/myid - Узнать ID этого чата\n" +
                "/setemail <...> - Добавить email\n" +
                "/lang - Установить язык интерфейса\n");
    }
}
