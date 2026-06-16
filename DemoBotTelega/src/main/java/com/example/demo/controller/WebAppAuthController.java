package com.example.demo.controller;

import com.example.demo.dto.TelegramUserDto;
import com.example.demo.dto.WebAppAuthRequest;
import com.example.demo.service.TelegramWebAppAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@RestController
@RequestMapping("/api/webapp")
public class WebAppAuthController {
    private final TelegramWebAppAuthService authService;

    private final RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    public WebAppAuthController(TelegramWebAppAuthService authService,
                                RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        this.authService = authService;
        this.requestMappingHandlerAdapter = requestMappingHandlerAdapter;
    }

    @RequestMapping("/auth")
    public ResponseEntity<TelegramUserDto> auth(@RequestBody WebAppAuthRequest request) {
        TelegramUserDto user = authService.validate(request.getInitData());
        return ResponseEntity.ok(user);
    }
}
