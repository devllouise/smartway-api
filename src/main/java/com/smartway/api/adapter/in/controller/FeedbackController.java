package com.smartway.api.adapter.in.controller;

import com.smartway.api.adapter.in.dto.request.FeedbackRequest;
import com.smartway.api.application.service.FeedbackService;
import com.smartway.api.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> submit(@AuthenticationPrincipal User user,
                                                      @Valid @RequestBody FeedbackRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            feedbackService.submit(user.getId(), r.getHistoryId(), r.getHelpful(), r.getChosenMode(), r.getComment()));
    }
}
