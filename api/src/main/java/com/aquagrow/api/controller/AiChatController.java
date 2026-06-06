package com.aquagrow.api.controller;

import com.aquagrow.api.dto.ChatRequest;
import com.aquagrow.api.dto.ChatResponse;
import com.aquagrow.api.service.OllamaChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AiChatController {

    private final OllamaChatService ollamaChatService;

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return ollamaChatService.chat(request);
    }

    @GetMapping("/models")
    public Map<String, Object> listModels() {
        return ollamaChatService.listModels();
    }
}
