package com.smartway.api.adapter.in.controller;

import com.smartway.api.adapter.in.dto.request.FeedbackRequest;
import com.smartway.api.adapter.in.dto.response.FeedbackResponse;
import com.smartway.api.application.service.FeedbackService;
import com.smartway.api.domain.entity.Feedback;
import com.smartway.api.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @AuthenticationPrincipal User user,
            @RequestBody FeedbackRequest request) {
        Feedback feedback = feedbackService.submitFeedback(
                user.getId(),
                request.getConsultationId(),
                request.getRecommendationHelpful(),
                request.getChosenMode(),
                request.getComment());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                FeedbackResponse.builder().id(feedback.getId())
                        .message("Feedback registrado! Obrigado por ajudar a Bibi a aprender.")
                        .processed(true).build());
    }
}
