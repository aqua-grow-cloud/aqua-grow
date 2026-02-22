package com.aquagrow.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TelegramMessageService {

    private final RestTemplate restTemplate;
    private final String botToken;

    public TelegramMessageService(RestTemplate restTemplate, @Value("${telegram.bot.token}") String botToken) {
        this.restTemplate = restTemplate;
        this.botToken = botToken;
    }

    public void sendMessage(String chatId, String text) {
        if (botToken == null || botToken.equals("your_bot_token_here")) {
            log.warn("Telegram bot token not configured. Cannot send message to {}", chatId);
            return;
        }

        String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

        Map<String, String> payload = new HashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", text);
        payload.put("parse_mode", "HTML");

        try {
            @SuppressWarnings("null")
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(url, payload,
                    String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully sent Telegram message to {}", chatId);
            } else {
                log.warn("Failed to send Telegram message to {}. Status: {}", chatId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send Telegram message to {}", chatId, e);
        }
    }
}
