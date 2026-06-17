package com.example.demo.dto;

public class TelegramUserDto {
    private Long telegramId;
    private String username;
    private String firstName;

    public TelegramUserDto(Long telegramId, String username, String firstName) {
        this.telegramId = telegramId;
        this.username = username;
        this.firstName = firstName;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }
}