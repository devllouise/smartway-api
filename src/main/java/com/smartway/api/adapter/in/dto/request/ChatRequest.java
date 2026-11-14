package com.smartway.api.adapter.in.dto.request;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String conversationContext;
}
