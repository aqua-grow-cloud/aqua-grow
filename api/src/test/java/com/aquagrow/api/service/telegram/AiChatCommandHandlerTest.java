package com.aquagrow.api.service.telegram;

import com.aquagrow.api.dto.ChatMessage;
import com.aquagrow.api.dto.ChatResponse;
import com.aquagrow.api.service.OllamaChatService;
import com.aquagrow.api.service.TelegramMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatCommandHandlerTest {

    @Mock
    private OllamaChatService ollamaChatService;

    @Mock
    private TelegramMessageService telegramMessageService;

    @InjectMocks
    private AiChatCommandHandler handler;

    @BeforeEach
    void setUp() {
        ChatResponse response = ChatResponse.builder()
                .message(ChatMessage.builder().role("assistant").content("Keep pH between 6.5-8.5 for tilapia.").build())
                .build();
        when(ollamaChatService.chat(any())).thenReturn(response);
    }

    @Test
    void canHandle_shouldReturnTrue_forAskPrefix() {
        assertThat(handler.canHandle("ASK What is pH?")).isTrue();
        assertThat(handler.canHandle("ask what is pH?")).isTrue();
        assertThat(handler.canHandle("Ask about tilapia")).isTrue();
    }

    @Test
    void canHandle_shouldReturnFalse_forOtherText() {
        assertThat(handler.canHandle("F B1 Feed 100 kg")).isFalse();
        assertThat(handler.canHandle("/help")).isFalse();
        assertThat(handler.canHandle("S B1 Tilapia 10 2.5")).isFalse();
    }

    @Test
    void handle_shouldSendQuestionToOllamaAndReply() {
        handler.handle("12345", "ASK What is ideal pH for tilapia?");

        verify(ollamaChatService).chat(argThat(req ->
                req.getMessages().size() == 1
                        && req.getMessages().get(0).getContent().equals("What is ideal pH for tilapia?")
        ));
        verify(telegramMessageService).sendMessage("12345", "Keep pH between 6.5-8.5 for tilapia.");
    }

    @Test
    void handle_shouldSendErrorMessage_whenOllamaFails() {
        when(ollamaChatService.chat(any())).thenThrow(new RuntimeException("Connection refused"));

        handler.handle("12345", "ASK Hello");

        verify(telegramMessageService).sendMessage(eq("12345"), argThat(msg -> msg.contains("AI service error")));
    }

    @Test
    void handle_shouldSendUsage_whenQuestionIsEmpty() {
        handler.handle("12345", "ASK ");

        verify(telegramMessageService).sendMessage(eq("12345"), argThat(msg -> msg.contains("Usage")));
        verifyNoInteractions(ollamaChatService);
    }

    @Test
    void handle_shouldSendUsage_whenOnlyPrefix() {
        handler.handle("12345", "ASK");

        verify(telegramMessageService).sendMessage(eq("12345"), argThat(msg -> msg.contains("Usage")));
        verifyNoInteractions(ollamaChatService);
    }
}
