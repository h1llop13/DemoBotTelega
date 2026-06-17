package com.example.demo.service;

import com.example.demo.dto.TelegramUserDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TelegramWebAppAuthService {

    private static final long MAX_INIT_DATA_AGE_SECONDS = 86_400; // 24 часа

    @Value("${telegram.bot.token}")
    private String botToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TelegramUserDto validate(String initData) {
        try {
            Map<String, String> params = parseParams(initData);
            String hash = params.remove("hash");

            // Формируем data-check-string
            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));

            // Вычисляем HMAC-SHA256
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    hmac("WebAppData", botToken.getBytes(StandardCharsets.UTF_8)),
                    "HmacSHA256"
            ));
            byte[] computed = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            String computedHash = bytesToHex(computed);

            // Constant-time сравнение хэшей
            if (!MessageDigest.isEqual(
                    computedHash.getBytes(StandardCharsets.UTF_8),
                    hash.getBytes(StandardCharsets.UTF_8))) {
                throw new RuntimeException("Invalid Telegram signature");
            }

            // Проверка срока жизни подписи (защита от replay чужой initData)
            String authDateStr = params.get("auth_date");
            if (authDateStr == null) {
                throw new RuntimeException("Missing auth_date");
            }
            long authDate = Long.parseLong(authDateStr);
            long now = Instant.now().getEpochSecond();
            if (now - authDate > MAX_INIT_DATA_AGE_SECONDS) {
                throw new RuntimeException("initData expired");
            }

            // Парсим user через Jackson вместо ручного парсинга
            String userJson = params.get("user");
            JsonNode userNode = objectMapper.readTree(userJson);
            long id = userNode.get("id").asLong();
            String username = userNode.hasNonNull("username") ? userNode.get("username").asText() : null;
            String firstName = userNode.get("first_name").asText();

            return new TelegramUserDto(id, username, firstName);
        } catch (Exception e) {
            throw new RuntimeException("initData validation failed: " + e.getMessage(), e);
        }
    }

    private byte[] hmac(String key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(data);
    }

    private Map<String, String> parseParams(String initData) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : initData.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, val);
            }
        }
        return map;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}