package com.smartway.api.application.service;

import com.smartway.api.adapter.out.persistence.ConsultationRepository;
import com.smartway.api.adapter.out.persistence.FeedbackRepository;
import com.smartway.api.adapter.out.persistence.UserRepository;
import com.smartway.api.application.exception.ResourceNotFoundException;
import com.smartway.api.domain.entity.Consultation;
import com.smartway.api.domain.entity.Feedback;
import com.smartway.api.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ConsultationRepository consultationRepository;
    private final LearningService learningService;

    @Transactional
    public Feedback submitFeedback(Long userId, Long consultationId, Boolean helpful, String chosenMode, String comment) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta nao encontrada"));

        Feedback feedback = Feedback.builder()
                .user(user).consultation(consultation)
                .recommendationHelpful(helpful).chosenMode(chosenMode).comment(comment).build();
        feedback = feedbackRepository.save(feedback);

        learningService.processFeedback(feedback, consultation.getDestinationType(), null, null);
        return feedback;
    }
}
