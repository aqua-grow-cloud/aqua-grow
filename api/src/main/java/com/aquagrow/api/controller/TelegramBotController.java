package com.aquagrow.api.controller;

import com.aquagrow.api.service.TelegramDataEntryService;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramBotController {

    private final TelegramDataEntryService telegramDataEntryService;

    @Value("${telegram.webhook.secret-token}")
    private String secretToken;

    @PostMapping("/webhook")
    public void receiveUpdate(@RequestBody TelegramUpdate update, HttpServletRequest request) {
        String receivedToken = request.getHeader("X-Telegram-Bot-Api-Secret-Token");

        if (secretToken != null && !secretToken.equals("a_very_secure_random_string")
                && !secretToken.equals(receivedToken)) {
            log.warn("Payload received with invalid secret token: {}", receivedToken);
            return;
        }

        log.info("Received Telegram update: {}", update);
        if (update.getMessage() != null && update.getMessage().getText() != null) {
            String chatId = String.valueOf(update.getMessage().getChat().getId());
            String text = update.getMessage().getText();
            telegramDataEntryService.processMessage(chatId, text);
        }
    }

    @Data
    public static class TelegramUpdate {
        private Long updateId;
        private TelegramMessage message;
    }

    @Data
    public static class TelegramMessage {
        @JsonProperty("message_id")
        private Long messageId;
        private TelegramChat chat;
        private String text;
    }

    @Data
    public static class TelegramChat {
        private Long id;
        private String type;
    }
}
