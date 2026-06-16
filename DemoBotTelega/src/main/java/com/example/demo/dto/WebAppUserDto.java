package com.example.demo.dto;

public class WebAppUserDto {
    private Long telegramId;
    private String username;
    private String firstName;
    private String email;

    public WebAppUserDto(Long telegramId, String username, String firstName, String email) {
        this.telegramId = telegramId;
        this.username = username;
        this.firstName = firstName;
        this.email = email;
    }

    public Long getTelegramId() { return telegramId; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getEmail() { return email; }
}
