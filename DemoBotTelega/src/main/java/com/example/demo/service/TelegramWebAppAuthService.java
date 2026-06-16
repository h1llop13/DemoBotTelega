package com.example.demo.service;

import com.example.demo.dto.TelegramUserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TelegramWebAppAuthService {

    @Value("${telegram.bot.token}")
    private String botToken;

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

            if (!computedHash.equals(hash)) {
                throw new RuntimeException("Invalid Telegram signature");
            }

            // Парсим user из JSON вручную (без Jackson-зависимости)
            String userJson = params.get("user");
            long id = extractLong(userJson, "id");
            String username = extractString(userJson, "username");
            String firstName = extractString(userJson, "first_name");

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

    private long extractLong(String json, String key) {
        String pattern = "\"" + key + "\":";
        int i = json.indexOf(pattern) + pattern.length();
        int j = json.indexOf(',', i);
        if (j == -1) j = json.indexOf('}', i);
        return Long.parseLong(json.substring(i, j).trim());
    }

    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int i = json.indexOf(pattern);
        if (i == -1) return null;
        i += pattern.length();
        int j = json.indexOf('"', i);
        return json.substring(i, j);
    }
}