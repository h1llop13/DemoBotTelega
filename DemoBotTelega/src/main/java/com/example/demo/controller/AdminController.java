package com.example.demo.controller;

import com.example.demo.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admins")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<?> addAdmin(@RequestBody Map<String, Long> request) {
        Long chatId = request.get("chatId");
        if (chatId == null) {
            return  ResponseEntity.badRequest().body(Map.of("error", "chatId is required"));
        }
        adminService.addAdmin(chatId);
        return ResponseEntity.ok(Map.of("status", "admin added", "chatId", chatId));
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<?> removeAdmin(@PathVariable Long chatId) {
        adminService.removeAdmin(chatId);
        return ResponseEntity.ok(Map.of("status", "admin removed", "chatId", chatId));
    }

}
