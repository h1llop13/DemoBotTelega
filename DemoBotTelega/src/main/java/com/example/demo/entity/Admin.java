package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", unique = true, nullable = false)
    private Long chatId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Admin() {}

    public Admin(Long chatId) {
        this.chatId = chatId;
        this.createdAt = LocalDateTime.now();
    }

}
