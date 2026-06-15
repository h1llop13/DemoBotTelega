package com.example.demo;

import com.example.demo.entity.Machine;
import com.example.demo.repo.MachineRepository;
import com.example.demo.service.MachineEventService;
import com.example.demo.service.MachineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MachineServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @InjectMocks
    private MachineService machineService;

    @Test
    void shouldReturnAllMachineIds() {
        when(machineRepository.findAll()).thenReturn(List.of(
                new Machine("MACHINE-001"),
                new Machine("MACHINE-002")
        ));

        List<String> result = machineService.getAllMachineIds();

        assertEquals(2, result.size());
        assertTrue(result.contains("MACHINE-001"));
        assertTrue(result.contains("MACHINE-002"));
    }

    @Test
    void shouldAddMachineIfNotExists() {
        when(machineRepository.existsByMachineId("MACHINE-003")).thenReturn(false);

        machineService.addMachine("MACHINE-003");

        verify(machineRepository).save(any(Machine.class));
    }

    @Test
    void shouldNotAddDuplicateMachine() {
        when(machineRepository.existsByMachineId("MACHINE-001")).thenReturn(true);

        machineService.addMachine("MACHINE-001");

        verify(machineRepository, never()).save(any());
    }

    @Test
    void shouldDeleteMachineAndReturnTrue() {
        Machine machine = new Machine("MACHINE-001");
        when(machineRepository.findByMachineId("MACHINE-001")).thenReturn(Optional.of(machine));

        boolean result = machineService.deleteMachine("MACHINE-001");

        assertTrue(result);
        verify(machineRepository).delete(machine);
    }

    @Test
    void shouldReturnFalseWhenMachineNotFound() {
        when(machineRepository.findByMachineId("UNKNOWN")).thenReturn(Optional.empty());

        boolean result = machineService.deleteMachine("UNKNOWN");

        assertFalse(result);
        verify(machineRepository, never()).delete(any());
    }
}
