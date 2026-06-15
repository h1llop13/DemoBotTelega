package com.example.demo.handler;

import com.example.demo.bot.MachineBot;
import com.example.demo.service.LocaleService;
import com.example.demo.service.SubscriberService;

public class LanguageCallbackHandler implements CallbackHandler {

    private final SubscriberService subscriberService;
    private final LocaleService localeService;
    private final MachineBot bot;

    public LanguageCallbackHandler(SubscriberService subscriberService, LocaleService localeService, MachineBot bot) {
        this.subscriberService = subscriberService;
        this.localeService = localeService;
        this.bot = bot;
    }

    @Override
    public void handle(Long chatId, String data) {
        // data приходит в формате "SETLANG_ru", "SETLANG_en", "SETLANG_nl"
        String newLang = data.replace("SETLANG_", "");

        // Обновляем язык пользователя в базе данных
        subscriberService.updateLanguage(chatId, newLang);

        // Отправляем подтверждение (метод msg сам подтянет уже новый измененный язык из БД)
        bot.send(chatId, localeService.msg(chatId, "bot.language_changed"));

        // Возвращаем пользователя в главное меню
        bot.sendMainMenu(chatId);
    }
}