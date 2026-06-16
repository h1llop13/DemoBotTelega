package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.Subscriber;
import com.example.demo.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/webapp")
public class WebAppApiController {

    private final TelegramWebAppAuthService authService;
    private final SubscriberService subscriberService;
    private final SubscriptionService subscriptionService;
    private final MachineService machineService;

    public WebAppApiController(TelegramWebAppAuthService authService,
                               SubscriberService subscriberService,
                               SubscriptionService subscriptionService,
                               MachineService machineService) {
        this.authService = authService;
        this.subscriberService = subscriberService;
        this.subscriptionService = subscriptionService;
        this.machineService = machineService;
    }

    // Достаём telegramId из initData header
    private Long resolveId(String initData) {
        return authService.validate(initData).getTelegramId();
    }

    @PostMapping("/auth")
    public ResponseEntity<TelegramUserDto> auth(@RequestBody WebAppAuthRequest req) {
        return ResponseEntity.ok(authService.validate(req.getInitData()));
    }

    @GetMapping("/me")
    public ResponseEntity<WebAppUserDto> me(@RequestHeader("X-Init-Data") String initData) {
        Long tgId = resolveId(initData);
        Subscriber sub = subscriberService.getByChatId(tgId);
        if (sub == null) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(new WebAppUserDto(tgId, sub.getUsername(), sub.getUsername(), sub.getEmail()));
    }

    @GetMapping("/machines")
    public ResponseEntity<List<WebAppMachineDto>> machines(@RequestHeader("X-Init-Data") String initData) {
        Long tgId = resolveId(initData);
        Subscriber sub = subscriberService.getByChatId(tgId);
        List<String> subscribed = sub != null ? subscriptionService.listMachines(sub.getId()) : List.of();
        List<String> all = machineService.getAllMachineIds();
        List<WebAppMachineDto> result = all.stream()
                .map(id -> new WebAppMachineDto(id, subscribed.contains(id)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-subscriptions")
    public ResponseEntity<List<WebAppMachineDto>> mySubscriptions(@RequestHeader("X-Init-Data") String initData) {
        Long tgId = resolveId(initData);
        Subscriber sub = subscriberService.getByChatId(tgId);
        if (sub == null) return ResponseEntity.ok(List.of());
        List<String> subscribed = subscriptionService.listMachines(sub.getId());
        List<WebAppMachineDto> result = subscribed.stream()
                .map(id -> new WebAppMachineDto(id, true))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/subscribe/{machineId}")
    public ResponseEntity<Void> subscribe(@RequestHeader("X-Init-Data") String initData,
                                          @PathVariable String machineId) {
        Long tgId = resolveId(initData);
        Subscriber sub = subscriberService.getByChatId(tgId);
        if (sub == null) return ResponseEntity.status(404).build();
        subscriptionService.subscribe(sub.getId(), machineId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe/{machineId}")
    public ResponseEntity<Void> unsubscribe(@RequestHeader("X-Init-Data") String initData,
                                            @PathVariable String machineId) {
        Long tgId = resolveId(initData);
        Subscriber sub = subscriberService.getByChatId(tgId);
        if (sub == null) return ResponseEntity.status(404).build();
        subscriptionService.unsubscribe(sub.getId(), machineId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/set-email")
    public ResponseEntity<Void> setEmail(@RequestHeader("X-Init-Data") String initData,
                                         @RequestBody java.util.Map<String, String> body) {
        Long tgId = resolveId(initData);
        subscriberService.setEmail(tgId, body.get("email"));
        return ResponseEntity.ok().build();
    }
}