package com.smartway.api.adapter.in.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FeedbackResponse {
    private Long id;
    private String message;
    private boolean processed;
}
