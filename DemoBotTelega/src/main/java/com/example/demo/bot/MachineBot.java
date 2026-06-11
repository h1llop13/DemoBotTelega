package com.example.demo.bot;

import com.example.demo.handler.*;
import com.example.demo.service.AdminService;
import com.example.demo.service.MachineEventService;
import com.example.demo.service.MachineService;
import com.example.demo.service.SubscriberService;
import com.example.demo.service.SubscriptionService;
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

    private final Map<Long, String> adminState = new HashMap<>();

    private final Map<String, CallbackHandler> exactHandlers = new HashMap<>();
    private final Map<String, CallbackHandler> prefixHandlers = new HashMap<>();

    private AdminEventFlowHandler adminEventFlowHandler;

    public MachineBot(SubscriberService subscriberService,
                      SubscriptionService subscriptionService,
                      MachineService machineService,
                      AdminService adminService,
                      MachineEventService machineEventService) {
        this.subscriberService = subscriberService;
        this.subscriptionService = subscriptionService;
        this.machineService = machineService;
        this.adminService = adminService;
        this.machineEventService = machineEventService;
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

        adminEventFlowHandler = new AdminEventFlowHandler(this, machineService, machineEventService);
        prefixHandlers.put("EVENT_", adminEventFlowHandler);
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

        // Удаляем сообщение, на котором была нажата кнопка
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

        send(chatId, "Неизвестная команда");
    }

    // === Обработка текстовых сообщений ===
    private void handleMessage(Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String user = update.getMessage().getFrom().getUserName();

        if (text == null) return;

        if (text.equals("/start")) {
            subscriberService.register(chatId, user);
            sendMenu(chatId, "Добро пожаловать!");
            return;
        }

        if (text.equals("/myid")) {
            send(chatId, "Ваш chatId: " + chatId);
            return;
        }

        if (text.equals("/admin")) {
            if (!isAdmin(chatId)) {
                send(chatId, "Нет доступа!");
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
                send(chatId, "Станок добавлен: " + text);
                return;
            }

            if (state.equals("WAIT_DELETE_MACHINE_NAME")) {
                boolean deleted = machineService.deleteMachine(text);
                adminState.remove(chatId);
                send(chatId, deleted ? "Станок удалён: " + text : "Станок не найден: " + text);
                return;
            }
        }

        if (text.startsWith("/subscribe")) {
            String[] parts = text.split(" ");
            if (parts.length < 2) {
                send(chatId, "Использование:\n/subscribe MACHINE-001");
                return;
            }
            var sub = subscriberService.getByChatId(chatId);
            subscriptionService.subscribe(sub.getId(), parts[1]);
            send(chatId, "Подписка оформлена: " + parts[1]);
            return;
        }

        if (text.startsWith("/unsubscribe")) {
            String[] parts = text.split(" ");
            if (parts.length < 2) {
                send(chatId, "Использование:\n/unsubscribe MACHINE-001");
                return;
            }
            var sub = subscriberService.getByChatId(chatId);
            subscriptionService.unsubscribe(sub.getId(), parts[1]);
            send(chatId, "Отписка: " + parts[1]);
            return;
        }

        if (text.equals("/list")) {
            var sub = subscriberService.getByChatId(chatId);
            var list = subscriptionService.listMachines(sub.getId());

            if (list.isEmpty()) {
                send(chatId, "У вас нет подписок");
                return;
            }

            StringBuilder sb = new StringBuilder("Ваши подписки:\n");
            for (String m : list) {
                sb.append("• Станок №").append(m).append("\n");
            }
            send(chatId, sb.toString());
            return;
        }

        if (text.startsWith("/setemail")) {
            String[] parts = text.split(" ");
            if (parts.length < 2) {
                send(chatId, "Использование:\n/setemail your@email.com");
                return;
            }
            String email = parts[1];
            subscriberService.setEmail(chatId, email);
            send(chatId, "Email сохранён: " + email + "\nТеперь уведомления о станках будут приходить на почту");
        }
    }

    // === Вспомогательные методы для хендлеров ===

    public Map<Long, String> getAdminState() {
        return adminState;
    }

    public boolean isAdmin(Long chatId) {
        return adminService.isAdmin(chatId);
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
        }
        catch (TelegramApiException e) {
            System.err.println("Не удалось удалить сообщение: " + messageId + ": " + e.getMessage());
        }
    }

    public void sendMainMenu(Long chatId) {
        sendMenu(chatId, "Главное меню");
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
            send(chatId, "Нет доступных машин!");
            return;
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String m : machines) {
            rows.add(List.of(btn("📡 " + m, "SUB_" + m)));
        }
        rows.add(List.of(btn("🔙 Назад", "BACK_TO_MAIN")));
        sendWithKeyboard(chatId, "Выберите станок:", rows);
    }

    public void showSubscriptions(Long chatId) {
        var sub = subscriberService.getByChatId(chatId);
        var list = subscriptionService.listMachines(sub.getId());

        if (list.isEmpty()) {
            send(chatId, "Нет подписок");
            return;
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String m : list) {
            rows.add(List.of(
                    btn("📡 " + m, "INFO_" + m),
                    btn("❌", "UNSUB_" + m)
            ));
        }
        rows.add(List.of(btn("🔙 Назад", "BACK_TO_MAIN")));
        sendWithKeyboard(chatId, "Ваши подписки:", rows);
    }

    public void sendAdminMenu(Long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(btn("Добавить станок", "ADMIN_ADD_MACHINE")),
                List.of(btn("Удалить станок", "ADMIN_DELETE_MACHINE")),
                List.of(btn("📤 Отправить событие", "EVENT_OPEN"))
        ));
        sendWithKeyboard(chatId, "Админ-панель", markup);
    }

    public void sendMenu(Long chatId, String text) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(btn("Подписаться", "MENU_SUBSCRIBE")),
                List.of(btn("Мои подписки", "MENU_LIST")),
                List.of(btn("Помощь", "MENU_HELP"))
        ));
        sendWithKeyboard(chatId, text, markup);
    }

    public InlineKeyboardButton btn(String text, String data) {
        InlineKeyboardButton b = new InlineKeyboardButton();
        b.setText(text);
        b.setCallbackData(data);
        return b;
    }

    // === Методы TelegramLongPollingBot ===

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
