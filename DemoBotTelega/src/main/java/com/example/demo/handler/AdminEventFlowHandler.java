package com.example.demo.handler;

import com.example.demo.bot.MachineBot;
import com.example.demo.machine.MachineStatus;
import com.example.demo.machine.MachineStatusChangeEvent;
import com.example.demo.service.MachineEventService;
import com.example.demo.service.MachineService;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import static com.example.demo.machine.MachineStatus.*;
import java.util.stream.Collectors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
* Флоу:
* /event - выбор станка
*        - выбор prevState
*        - выбор newState
*        - отправка в MachineEventService
*/

public class AdminEventFlowHandler implements CallbackHandler {

    private static final List<MachineStatus> STATUSES = List.of(
            RUN,
            STOP,
            CONNECTION_LOST
    );

    private static final String BACK_DATA = "EVENT_BACK";

    private final MachineBot bot;
    private final MachineService machineService;
    private final MachineEventService machineEventService;

    public AdminEventFlowHandler(MachineBot bot,
                                 MachineService machineService,
                                 MachineEventService machineEventService) {
        this.bot = bot;
        this.machineService = machineService;
        this.machineEventService = machineEventService;
    }

    @Override
    public void handle(Long chatId, String data) {
        if (!bot.isAdmin(chatId)) {
            bot.send(chatId, "Нет доступа!");
            return;
        }

        if (data.equals(BACK_DATA)) {
            handleBack(chatId);
            return;
        }

        if (data.equals("EVENT_OPEN")) {
            showMachineList(chatId);

        } else if (data.startsWith("EVENT_MACHINE_")) {
            handleMachineSelected(chatId, data);

        } else if (data.startsWith("EVENT_PREV_")) {
            handlePrevStateSelected(chatId, data);

        } else if (data.startsWith("EVENT_NEW_")) {
            handleNewStateSelected(chatId, data);
        }
    }

    // Шаг 1: показать список станков (вызывается из бота по команде /event)
    public void showMachineList(Long chatId) {
        if (!bot.isAdmin(chatId)) {
            bot.send(chatId, "Нет доступа!");
            return;
        }

        List<String> machines = machineService.getAllMachineIds();
        if (machines.isEmpty()) {
            bot.send(chatId, "Нет станков в системе. Сначала добавьте станок через /admin.");
            return;
        }

        List<List<InlineKeyboardButton>> rows = machines.stream()
                .map(m -> List.of(bot.btn("⚙ " + m, "EVENT_MACHINE_" + m)))
                .collect(Collectors.toList());
        rows.add(List.of(bot.btn("🔙 Назад", BACK_DATA)));

        bot.sendWithKeyboard(chatId, "Выберите станок для отправки события:", rows);
    }

    // Шаг 2: станок выбран — спрашиваем предыдущий статус
    private void handleMachineSelected(Long chatId, String data) {
        String machineId = data.replace("EVENT_MACHINE_", "");

        bot.getAdminState().put(chatId, "EVENT_WAIT_PREV:" + machineId);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (MachineStatus s : STATUSES) {
            rows.add(List.of(bot.btn(statusLabel(s), "EVENT_PREV_" + s.name())));
        }
        rows.add(List.of(bot.btn("🔙 Назад", BACK_DATA)));

        bot.sendWithKeyboard(chatId, "Станок: " + machineId + "\nВыберите предыдущий статус:", rows);
    }

    // Шаг 3: prevState выбран — спрашиваем новый статус
    private void handlePrevStateSelected(Long chatId, String data) {
        String state = bot.getAdminState().get(chatId);
        if (state == null || !state.startsWith("EVENT_WAIT_PREV:")) {
            bot.send(chatId, "Сначала выберите станок через /event");
            return;
        }

        String machineId = state.replace("EVENT_WAIT_PREV:", "");
        String prevState = data.replace("EVENT_PREV_", "");

        bot.getAdminState().put(chatId, "EVENT_WAIT_NEW:" + machineId + ":" + prevState);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (MachineStatus s : STATUSES) {
            rows.add(List.of(bot.btn(statusLabel(s), "EVENT_NEW_" + s.name())));
        }
        rows.add(List.of(bot.btn("🔙 Назад", BACK_DATA)));

        bot.sendWithKeyboard(chatId,
                "Станок: " + machineId + "\nПредыдущий: " + statusLabel(MachineStatus.valueOf(prevState)) +
                        "\nВыберите новый статус:", rows);
    }

    // Шаг 4: newState выбран — отправляем событие
    private void handleNewStateSelected(Long chatId, String data) {
        String state = bot.getAdminState().get(chatId);
        if (state == null || !state.startsWith("EVENT_WAIT_NEW:")) {
            bot.send(chatId, "Сначала выберите станок через /event");
            return;
        }

        String[] parts = state.replace("EVENT_WAIT_NEW:", "").split(":");
        if (parts.length < 2) {
            bot.send(chatId, "Ошибка состояния. Начните заново: /event");
            bot.getAdminState().remove(chatId);
            return;
        }

        String machineId = parts[0];
        MachineStatus prevState = MachineStatus.valueOf(parts[1]);
        MachineStatus newState = MachineStatus.valueOf(data.replace("EVENT_NEW_", ""));

        bot.getAdminState().remove(chatId);

        MachineStatusChangeEvent event = new MachineStatusChangeEvent(
                machineId,
                Instant.now(),
                prevState,
                newState
        );

        machineEventService.process(event);

        bot.send(chatId,
                "✅ Событие отправлено!\n" +
                        "Станок: " + machineId + "\n" +
                        statusLabel(prevState) + " → " + statusLabel(newState));
    }

    private void handleBack(Long chatId) {
        String state = bot.getAdminState().get(chatId);
        if (state == null) {
            bot.sendAdminMenu(chatId);
            return;
        }

        if (state.startsWith("EVENT_WAIT_PREV:")) {
            // Возврат к списку станков
            bot.getAdminState().remove(chatId);
            showMachineList(chatId);
        } else if (state.startsWith("EVENT_WAIT_NEW:")) {
            // Возврат к выбору предыдущего статуса
            String[] parts = state.replace("EVENT_WAIT_NEW:", "").split(":");
            if (parts.length >= 1) {
                String machineId = parts[0];
                bot.getAdminState().put(chatId, "EVENT_WAIT_PREV:" + machineId);
                // Показываем выбор предыдущего статуса
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                for (MachineStatus s : STATUSES) {
                    rows.add(List.of(bot.btn(statusLabel(s), "EVENT_PREV_" + s.name())));
                }
                rows.add(List.of(bot.btn("🔙 Назад", BACK_DATA)));
                bot.sendWithKeyboard(chatId, "Станок: " + machineId + "\nВыберите предыдущий статус:", rows);
            } else {
                bot.sendAdminMenu(chatId);
            }
        } else {
            bot.sendAdminMenu(chatId);
        }
    }

    // Читаемые названия статусов
    private String statusLabel(MachineStatus status) {
        return switch (status) {
            case RUN -> "▶ RUN";
            case STOP -> "⏹ STOP";
            case CONNECTION_LOST -> "❌ CONNECTION_LOST";
        };
    }

    // Сохраняем доступ к adminState map при инициализации
    public Map<Long, String> getStateMap() {
        return bot.getAdminState();
    }
}
