package com.example.demo;

import com.example.demo.bot.MachineBot;
import com.example.demo.entity.Machine;
import com.example.demo.handler.AdminEventFlowHandler;
import com.example.demo.machine.MachineStatus;
import com.example.demo.machine.MachineStatusChangeEvent;
import com.example.demo.service.MachineEventService;
import com.example.demo.service.MachineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminEventFlowHandlerTest {

    @Mock private MachineBot bot;
    @Mock private MachineService machineService;
    @Mock private MachineEventService machineEventService;

    private AdminEventFlowHandler handler;
    private Map<Long, String> adminState;

    private static final Long ADMIN_ID = 1000L;
    private static final Long NON_ADMIN_ID = 999L;

    @BeforeEach
    void setUp() {
        adminState = new HashMap<>();
        when(bot.getAdminState()).thenReturn(adminState);
        handler = new AdminEventFlowHandler(bot, machineService, machineEventService);
    }

    @Test
    void shouldDenyNonAdmin() {
        when(bot.isAdmin(NON_ADMIN_ID)).thenReturn(false);

        handler.handle(NON_ADMIN_ID, "EVENT_OPEN");

        verify(bot).send(eq(NON_ADMIN_ID), contains("доступ"));
        verify(machineService, never()).getAllMachineIds();
    }

    @Test
    void shouldShowMachineListToAdmin() {
        when(bot.isAdmin(ADMIN_ID)).thenReturn(true);
        when(machineService.getAllMachineIds()).thenReturn(List.of("MACHINE-001", "MACHINE-002"));
        when(bot.btn(anyString(), anyString())).thenAnswer(inv -> {
            var b = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
            b.setText(inv.getArgument(0));
            b.setCallbackData(inv.getArgument(1));
            return b;
        });

        handler.showMachineList(ADMIN_ID);

        verify(bot).sendWithKeyboard(eq(ADMIN_ID), anyString(), anyList());
    }

    @Test
    void shouldNotifyWhenNoMachinesAvailable() {
        when(bot.isAdmin(ADMIN_ID)).thenReturn(true);
        when(machineService.getAllMachineIds()).thenReturn(List.of());

        handler.showMachineList(ADMIN_ID);

        verify(bot).send(eq(ADMIN_ID), anyString());
        verify(bot, never()).sendWithKeyboard(anyLong(), anyString(), anyList());
    }

    @Test
    void shouldSetWaitPrevStateAfterMachineSelected() {
        when(bot.isAdmin(ADMIN_ID)).thenReturn(true);
        when(bot.btn(anyString(), anyString())).thenAnswer(inv -> {
            var b = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
            b.setText(inv.getArgument(0));
            b.setCallbackData(inv.getArgument(1));
            return b;
        });

        handler.handle(ADMIN_ID, "EVENT_MACHINE_MACHINE-001");

        assertEquals("EVENT_WAIT_PREV:MACHINE-001", adminState.get(ADMIN_ID));
    }

    @Test
    void shouldSetWaitNewStateAfterPrevStateSelected() {
        when(bot.isAdmin(ADMIN_ID)).thenReturn(true);
        adminState.put(ADMIN_ID, "EVENT_WAIT_PREV:MACHINE-001");
        when(bot.btn(anyString(), anyString())).thenAnswer(inv -> {
            var b = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
            b.setText(inv.getArgument(0));
            b.setCallbackData(inv.getArgument(1));
            return b;
        });

        handler.handle(ADMIN_ID, "EVENT_PREV_STOP");

        assertEquals("EVENT_WAIT_NEW:MACHINE-001:STOP", adminState.get(ADMIN_ID));
    }

    @Test
    void shouldProcessEventAfterNewStateSelected() {
        when(bot.isAdmin(ADMIN_ID)).thenReturn(true);
        adminState.put(ADMIN_ID, "EVENT_WAIT_NEW:MACHINE-001:STOP");

        handler.handle(ADMIN_ID, "EVENT_NEW_RUN");

        ArgumentCaptor<MachineStatusChangeEvent> captor = ArgumentCaptor.forClass(MachineStatusChangeEvent.class);
        verify(machineEventService).process(captor.capture());
        MachineStatusChangeEvent event = captor.getValue();
        assertEquals("MACHINE-001", event.machineId());
        assertEquals(MachineStatus.STOP, event.prevState());
        assertEquals(MachineStatus.RUN, event.newState());
    }

    @Test
    void shouldClearAdminStateAfterEventDispatched() {
        when(bot.isAdmin(ADMIN_ID)).thenReturn(true);
        adminState.put(ADMIN_ID, "EVENT_WAIT_NEW:MACHINE-001:RUN");

        handler.handle(ADMIN_ID, "EVENT_NEW_STOP");

        assertNull(adminState.get(ADMIN_ID));
    }

    @Test
    void shouldRejectPrevStateWhenNoMachineSelected() {
        when(bot.isAdmin(ADMIN_ID)).thenReturn(true);
        // adminState пустой — станок не выбран

        handler.handle(ADMIN_ID, "EVENT_PREV_STOP");

        verify(bot).send(eq(ADMIN_ID), anyString());
        verify(machineEventService, never()).process(any());
    }

    @Test
    void shouldRejectNewStateWhenNoPrevStateSet() {
        when(bot.isAdmin(ADMIN_ID)).thenReturn(true);
        // adminState пустой

        handler.handle(ADMIN_ID, "EVENT_NEW_RUN");

        verify(bot).send(eq(ADMIN_ID), anyString());
        verify(machineEventService, never()).process(any());
    }

    @Test
    void shouldGoToAdminMenuOnBackWithNoState() {
        when(bot.isAdmin(ADMIN_ID)).thenReturn(true);

        handler.handle(ADMIN_ID, "EVENT_BACK");

        verify(bot).sendAdminMenu(ADMIN_ID);
    }

    @Test
    void shouldGoBackToMachineListFromWaitPrevStep() {
        when(bot.isAdmin(ADMIN_ID)).thenReturn(true);
        adminState.put(ADMIN_ID, "EVENT_WAIT_PREV:MACHINE-001");
        when(machineService.getAllMachineIds()).thenReturn(List.of("MACHINE-001"));
        when(bot.btn(anyString(), anyString())).thenAnswer(inv -> {
            var b = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
            b.setText(inv.getArgument(0));
            b.setCallbackData(inv.getArgument(1));
            return b;
        });

        handler.handle(ADMIN_ID, "EVENT_BACK");

        // state должен быть очищен
        assertNull(adminState.get(ADMIN_ID));
        // список станков показан снова
        verify(bot).sendWithKeyboard(eq(ADMIN_ID), anyString(), anyList());
    }

    @Test
    void shouldGoBackToPrevStateSelectionFromWaitNewStep() {
        when(bot.isAdmin(ADMIN_ID)).thenReturn(true);
        adminState.put(ADMIN_ID, "EVENT_WAIT_NEW:MACHINE-001:STOP");
        when(bot.btn(anyString(), anyString())).thenAnswer(inv -> {
            var b = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
            b.setText(inv.getArgument(0));
            b.setCallbackData(inv.getArgument(1));
            return b;
        });

        handler.handle(ADMIN_ID, "EVENT_BACK");

        // state должен вернуться к WAIT_PREV
        assertEquals("EVENT_WAIT_PREV:MACHINE-001", adminState.get(ADMIN_ID));
    }

    @Test
    void getStateMapShouldReturnBotAdminState() {
        assertSame(adminState, handler.getStateMap());
    }
}