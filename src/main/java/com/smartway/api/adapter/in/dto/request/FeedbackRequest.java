package com.smartway.api.adapter.in.dto.request;

import lombok.Data;

@Data
public class FeedbackRequest {
    private Long consultationId;
    private Boolean recommendationHelpful;
    private String chosenMode;
    private String comment;
}
