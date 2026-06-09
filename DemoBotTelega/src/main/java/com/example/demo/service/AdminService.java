package com.example.demo.service;

import com.example.demo.entity.Admin;
import com.example.demo.repo.AdminRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    public boolean isAdmin(Long chatId) {
        return chatId != null && adminRepository.existsByChatId(chatId);
    }

    public void addAdmin(Long chatId) {
        if (!adminRepository.existsByChatId(chatId)) {
            adminRepository.save(new Admin(chatId));
        }
    }

    public void removeAdmin(Long chatId) {
        adminRepository.findByChatId(chatId).ifPresent(adminRepository::delete);
    }
}
