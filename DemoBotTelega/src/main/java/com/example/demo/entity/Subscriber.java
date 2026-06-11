package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long chatId;
    private String username;
    private String email;
    private LocalDateTime createdAt = LocalDateTime.now();
    public Subscriber() {}

    public Subscriber(Long id, Long chatId, String username, LocalDateTime createdAt) {
        this.id = id;
        this.chatId = chatId;
        this.username = username;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getChatId() {
        return chatId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }
}
