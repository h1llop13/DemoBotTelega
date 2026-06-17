package com.example.demo.bot;

import com.example.demo.handler.*;
import com.example.demo.service.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import java.util.ArrayList;
import java.util.List;

import java.util.*;

@Component
public class MachineBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String username;

    @Value("${telegram.bot.token}")
    private String token;

    private final SubscriberService subscriberService;
    private final SubscriptionService subscriptionService;
    private final MachineService machineService;
    private final AdminService adminService;
    private final MachineEventService machineEventService;
    private final LocaleService localeService;

    private final MachineEventProducer machineEventProducer;

    private final Map<Long, String> adminState = new HashMap<>();

    private final Map<String, CallbackHandler> exactHandlers = new HashMap<>();
    private final Map<String, CallbackHandler> prefixHandlers = new HashMap<>();

    private AdminEventFlowHandler adminEventFlowHandler;

    public MachineBot(SubscriberService subscriberService,
                      SubscriptionService subscriptionService,
                      MachineService machineService,
                      AdminService adminService,
                      MachineEventService machineEventService,
                      LocaleService localeService,
                      MachineEventProducer machineEventProducer) {
        this.subscriberService = subscriberService;
        this.subscriptionService = subscriptionService;
        this.machineService = machineService;
        this.adminService = adminService;
        this.machineEventService = machineEventService;
        this.localeService = localeService;
        this.machineEventProducer = machineEventProducer;
    }

    // === Инициализация хендлеров ===
    @PostConstruct
    private void initHandlers() {
        exactHandlers.put("MENU_SUBSCRIBE", new MenuSubscribeHandler(this));
        exactHandlers.put("MENU_LIST", new MenuListHandler(this));
        exactHandlers.put("MENU_HELP", new MenuHelpHandler(this));
        exactHandlers.put("ADMIN_ADD_MACHINE", new AdminAddMachineHandler(this));
        exactHandlers.put("ADMIN_DELETE_MACHINE", new AdminDeleteMachineHandler(this));
        exactHandlers.put("BACK_TO_MAIN", new BackHandler(this));

        prefixHandlers.put("SUB_", new SubscribeCallbackHandler(subscriberService, subscriptionService, this));
        prefixHandlers.put("UNSUB_", new UnsubscriberCallbackHandler(subscriberService, subscriptionService, this));
        prefixHandlers.put("INFO_", new InfoCallbackHandler(this));

        adminEventFlowHandler = new AdminEventFlowHandler(this, machineService, machineEventProducer);
        prefixHandlers.put("EVENT_", adminEventFlowHandler);

        // ВОТ ЭТУ СТРОКУ ДОБАВИТЬ:
        prefixHandlers.put("SETLANG_", new LanguageCallbackHandler(subscriberService, localeService, this));
    }

    // === Входящие обновления ===
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update);
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
        }


    }

    // === Обработка callback-кнопок ===
    private void handleCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        deleteMessage(chatId, messageId);

        CallbackHandler handler = exactHandlers.get(data);
        if (handler != null) {
            handler.handle(chatId, data);
            return;
        }

        for (Map.Entry<String, CallbackHandler> entry : prefixHandlers.entrySet()) {
            if (data.startsWith(entry.getKey())) {
                entry.getValue().handle(chatId, data);
                return;
            }
        }

        send(chatId, "Unknown command");
    }

    // === Обработка текстовых сообщений ===
    private void handleMessage(Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String user = update.getMessage().getFrom().getUserName();
        String langCode = update.getMessage().getFrom().getLanguageCode(); // "ru", "en", "nl" и т.д.

        if (text == null) return;

        if (text.equals("/start")) {
            subscriberService.register(chatId, user, langCode);
            sendMenu(chatId, localeService.msg(chatId, "bot.welcome"));
            return;
        }

        // ВОТ ЭТОТ БЛОК ДОБАВИТЬ:
        if (text.equals("/lang") || text.equals("/language")) {
            showLanguageMenu(chatId);
            return;
        }

        if (text.equals("/myid")) {
            send(chatId, localeService.msg(chatId, "bot.your_chat_id", chatId));
            return;
        }

        if (text.equals("/admin")) {
            if (!isAdmin(chatId)) {
                send(chatId, localeService.msg(chatId, "bot.access_denied"));
                return;
            }
            sendAdminMenu(chatId);
            return;
        }

        if (text.equals("/event")) {
            adminEventFlowHandler.showMachineList(chatId);
            return;
        }

        // === Ожидание ввода от администратора ===
        String state = adminState.get(chatId);

        if (state != null && isAdmin(chatId)) {
            if (state.equals("WAIT_MACHINE_NAME")) {
                machineService.addMachine(text);
                adminState.remove(chatId);
                send(chatId, localeService.msg(chatId, "bot.machine_added", text));
                return;
            }

            if (state.equals("WAIT_DELETE_MACHINE_NAME")) {
                boolean deleted = machineService.deleteMachine(text);
                adminState.remove(chatId);
                send(chatId, deleted
                        ? localeService.msg(chatId, "bot.machine_deleted", text)
                        : localeService.msg(chatId, "bot.machine_not_found", text));
                return;
            }
        }

        if (text.startsWith("/subscribe")) {
            String[] parts = text.split(" ");
            if (parts.length < 2) {
                send(chatId, localeService.msg(chatId, "bot.subscribe_usage"));
                return;
            }
            var sub = subscriberService.getByChatId(chatId);
            subscriptionService.subscribe(sub.getId(), parts[1]);
            send(chatId, localeService.msg(chatId, "bot.subscribed", parts[1]));
            return;
        }

        if (text.startsWith("/unsubscribe")) {
            String[] parts = text.split(" ");
            if (parts.length < 2) {
                send(chatId, localeService.msg(chatId, "bot.unsubscribe_usage"));
                return;
            }
            var sub = subscriberService.getByChatId(chatId);
            subscriptionService.unsubscribe(sub.getId(), parts[1]);
            send(chatId, localeService.msg(chatId, "bot.unsubscribed", parts[1]));
            return;
        }

        if (text.equals("/list")) {
            var sub = subscriberService.getByChatId(chatId);
            var list = subscriptionService.listMachines(sub.getId());

            if (list.isEmpty()) {
                send(chatId, localeService.msg(chatId, "bot.no_subscriptions"));
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (String m : list) {
                sb.append(localeService.msg(chatId, "bot.machine_prefix", m)).append("\n");
            }
            send(chatId, localeService.msg(chatId, "bot.subscriptions_title") + "\n" + sb);
            return;
        }

        if (text.startsWith("/setemail")) {
            String[] parts = text.split(" ");
            if (parts.length < 2) {
                send(chatId, localeService.msg(chatId, "bot.email_usage"));
                return;
            }
            String email = parts[1];
            subscriberService.setEmail(chatId, email);
            send(chatId, localeService.msg(chatId, "bot.email_saved", email));
        }
    }

    // === Вспомогательные методы ===

    public Map<Long, String> getAdminState() {
        return adminState;
    }

    public boolean isAdmin(Long chatId) {
        return adminService.isAdmin(chatId);
    }

    public LocaleService getLocaleService() {
        return localeService;
    }

    public void send(Long chatId, String text) {
        try {
            execute(new SendMessage(chatId.toString(), text));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void deleteMessage(Long chatId, Integer messageId) {
        try {
            execute(new DeleteMessage(chatId.toString(), messageId));
        } catch (TelegramApiException e) {
            System.err.println("Could not delete message " + messageId + ": " + e.getMessage());
        }
    }

    public void sendMainMenu(Long chatId) {
        sendMenu(chatId, localeService.msg(chatId, "bot.welcome"));
    }

    public void sendWithKeyboard(Long chatId, String text, InlineKeyboardMarkup markup) {
        try {
            SendMessage msg = new SendMessage(chatId.toString(), text);
            msg.setReplyMarkup(markup);
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendWithKeyboard(Long chatId, String text, List<List<InlineKeyboardButton>> rows) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        sendWithKeyboard(chatId, text, markup);
    }

    public void showMachines(Long chatId) {
        List<String> machines = machineService.getAllMachineIds();

        if (machines.isEmpty()) {
            send(chatId, localeService.msg(chatId, "bot.no_machines"));
            return;
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String m : machines) {
            rows.add(List.of(btn("📡 " + m, "SUB_" + m)));
        }
        rows.add(List.of(btn(localeService.msg(chatId, "bot.back"), "BACK_TO_MAIN")));
        sendWithKeyboard(chatId, localeService.msg(chatId, "bot.machines_title"), rows);
    }

    public void showSubscriptions(Long chatId) {
        var sub = subscriberService.getByChatId(chatId);
        var list = subscriptionService.listMachines(sub.getId());

        if (list.isEmpty()) {
            send(chatId, localeService.msg(chatId, "bot.no_subscriptions"));
            return;
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String m : list) {
            rows.add(List.of(
                    btn("📡 " + m, "INFO_" + m),
                    btn("❌", "UNSUB_" + m)
            ));
        }
        rows.add(List.of(btn(localeService.msg(chatId, "bot.back"), "BACK_TO_MAIN")));
        sendWithKeyboard(chatId, localeService.msg(chatId, "bot.subscriptions_title"), rows);
    }

    public void sendAdminMenu(Long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(btn(localeService.msg(chatId, "bot.admin_add_machine"), "ADMIN_ADD_MACHINE")),
                List.of(btn(localeService.msg(chatId, "bot.admin_delete_machine"), "ADMIN_DELETE_MACHINE")),
                List.of(btn(localeService.msg(chatId, "bot.admin_send_event"), "EVENT_OPEN"))
        ));
        sendWithKeyboard(chatId, localeService.msg(chatId, "bot.admin_panel"), markup);
    }

    public void sendMenu(Long chatId, String text) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton webAppBtn = new InlineKeyboardButton();
        webAppBtn.setText("Открыть mini App");
        webAppBtn.setWebApp(new WebAppInfo("https://flat-begins-lan-implement.trycloudflare.com"));

        markup.setKeyboard(List.of(
                List.of(btn(localeService.msg(chatId, "bot.menu.subscribe"), "MENU_SUBSCRIBE")),
                List.of(btn(localeService.msg(chatId, "bot.menu.my_subscriptions"), "MENU_LIST")),
                List.of(btn(localeService.msg(chatId, "bot.menu.help"), "MENU_HELP")),
                List.of(webAppBtn)
        ));

        sendWithKeyboard(chatId, text, markup);
    }

    public void showLanguageMenu(Long chatId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(btn("🇷🇺 Русский", "SETLANG_ru")));
        rows.add(List.of(btn("🇬🇧 English", "SETLANG_en")));
        rows.add(List.of(btn("🇳🇱 Nederlands", "SETLANG_nl")));
        rows.add(List.of(btn(localeService.msg(chatId, "bot.back"), "BACK_TO_MAIN")));

        sendWithKeyboard(chatId, localeService.msg(chatId, "bot.choose_language"), rows);
    }

    public InlineKeyboardButton btn(String text, String data) {
        InlineKeyboardButton b = new InlineKeyboardButton();
        b.setText(text);
        b.setCallbackData(data);
        return b;
    }

    @Override
    public String getBotUsername() { return username; }

    @Override
    public String getBotToken() { return token; }
}