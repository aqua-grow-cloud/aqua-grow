package com.aquagrow.api.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotEmpty(message = "Messages list must not be empty")
    private List<ChatMessage> messages;

    private String model;

    private Boolean stream;
}
