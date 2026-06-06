package com.aquagrow.api.service.telegram;

import com.aquagrow.api.dto.ChatMessage;
import com.aquagrow.api.dto.ChatRequest;
import com.aquagrow.api.dto.ChatResponse;
import com.aquagrow.api.service.OllamaChatService;
import com.aquagrow.api.service.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatCommandHandler implements TelegramCommandHandler {

    private final OllamaChatService ollamaChatService;
    private final TelegramMessageService telegramMessageService;

    private static final String PREFIX = "ASK ";

    @Override
    public boolean canHandle(String text) {
        return text.toUpperCase().startsWith(PREFIX);
    }

    @Override
    public void handle(String chatId, String text) {
        String question = text.substring(PREFIX.length()).trim();

        if (question.isEmpty()) {
            telegramMessageService.sendMessage(chatId,
                    "❌ Usage: <code>ASK [your question]</code>\n<i>Example: ASK What is the ideal pH for tilapia?</i>");
            return;
        }

        log.debug("Processing AI chat request from telegram chat {}: {}", chatId, question);

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.builder().role("user").content(question).build()
                ))
                .build();

        try {
            ChatResponse response = ollamaChatService.chat(request);
            String reply = response.getMessage() != null ? response.getMessage().getContent() : "No response from AI.";
            telegramMessageService.sendMessage(chatId, reply);
        } catch (Exception e) {
            log.error("AI chat failed for telegram chat {}: {}", chatId, e.getMessage());
            telegramMessageService.sendMessage(chatId, "❌ AI service error: " + e.getMessage());
        }
    }
}
