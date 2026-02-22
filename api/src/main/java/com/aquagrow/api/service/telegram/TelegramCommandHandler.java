package com.aquagrow.api.service.telegram;

public interface TelegramCommandHandler {
    boolean canHandle(String text);

    void handle(String chatId, String text);
}
