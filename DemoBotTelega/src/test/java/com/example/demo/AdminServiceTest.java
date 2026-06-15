package com.example.demo;

import com.example.demo.entity.Admin;
import com.example.demo.repo.AdminRepository;
import com.example.demo.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminService adminService;

    // isAdmin возвращает true для зарегистрированного администратора
    @Test
    void shouldReturnTrueForExistingAdmin() {
        when(adminRepository.existsByChatId(100L)).thenReturn(true);

        assertTrue(adminService.isAdmin(100L));
    }

    // isAdmin возвращает false для незарегистрированного пользователя
    @Test
    void shouldReturnFalseForNonAdmin() {
        when(adminRepository.existsByChatId(999L)).thenReturn(false);

        assertFalse(adminService.isAdmin(999L));
    }

    // isAdmin возвращает false при null chatId (защита от NPE)
    @Test
    void shouldReturnFalseForNullChatId() {
        assertFalse(adminService.isAdmin(null));
        verify(adminRepository, never()).existsByChatId(any());
    }

    // addAdmin сохраняет нового администратора
    @Test
    void shouldAddNewAdmin() {
        when(adminRepository.existsByChatId(200L)).thenReturn(false);

        adminService.addAdmin(200L);

        verify(adminRepository).save(any(Admin.class));
    }

    // addAdmin не создаёт дубликат, если администратор уже существует
    @Test
    void shouldNotAddDuplicateAdmin() {
        when(adminRepository.existsByChatId(100L)).thenReturn(true);

        adminService.addAdmin(100L);

        verify(adminRepository, never()).save(any());
    }

    // removeAdmin удаляет существующего администратора
    @Test
    void shouldRemoveExistingAdmin() {
        Admin admin = new Admin(100L);
        when(adminRepository.findByChatId(100L)).thenReturn(Optional.of(admin));

        adminService.removeAdmin(100L);

        verify(adminRepository).delete(admin);
    }

    // removeAdmin ничего не делает, если администратор не найден
    @Test
    void shouldDoNothingWhenRemovingNonExistentAdmin() {
        when(adminRepository.findByChatId(999L)).thenReturn(Optional.empty());

        adminService.removeAdmin(999L);

        verify(adminRepository, never()).delete(any());
    }
}