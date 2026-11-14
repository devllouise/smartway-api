package com.smartway.api.application.service;

import com.smartway.api.adapter.out.persistence.ContextPatternRepository;
import com.smartway.api.adapter.out.persistence.FeedbackRepository;
import com.smartway.api.adapter.out.persistence.PreferenceHistoryRepository;
import com.smartway.api.adapter.out.persistence.UserPreferenceRepository;
import com.smartway.api.domain.entity.ContextPattern;
import com.smartway.api.domain.entity.Feedback;
import com.smartway.api.domain.entity.PreferenceHistory;
import com.smartway.api.domain.entity.User;
import com.smartway.api.domain.entity.UserPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningService {
    private final FeedbackRepository feedbackRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final PreferenceHistoryRepository historyRepository;
    private final ContextPatternRepository patternRepository;

    @Value("${smartway.learning.min-interactions:10}")
    private int minInteractions;

    @Value("${smartway.learning.weight-adjustment-rate:0.05}")
    private double adjustmentRate;

    @Transactional
    public void processFeedback(Feedback feedback, String destinationType, String timeOfDay, String dayType) {
        updateContextPattern(feedback.getUser().getId(), destinationType, timeOfDay, dayType, feedback.getChosenMode());
        long totalFeedbacks = feedbackRepository.findByUserIdOrderByCreatedAtDesc(feedback.getUser().getId()).size();
        if (totalFeedbacks >= minInteractions && totalFeedbacks % 5 == 0) {
            adjustWeights(feedback.getUser().getId());
        }
    }

    private void updateContextPattern(Long userId, String destType, String timeOfDay, String dayType, String chosenMode) {
        if (destType == null || chosenMode == null) return;
        var existing = patternRepository.findByUserIdAndDestinationTypeAndTimeOfDayAndDayType(userId, destType, timeOfDay, dayType);
        if (existing.isPresent()) {
            ContextPattern p = existing.get();
            p.setOccurrenceCount(p.getOccurrenceCount() + 1);
            p.setMostChosenMode(chosenMode);
            p.setLastSeenAt(LocalDateTime.now());
            patternRepository.save(p);
        } else {
            patternRepository.save(ContextPattern.builder()
                    .user(User.builder().id(userId).build())
                    .destinationType(destType).timeOfDay(timeOfDay).dayType(dayType)
                    .mostChosenMode(chosenMode).occurrenceCount(1).lastSeenAt(LocalDateTime.now()).build());
        }
    }

    private void adjustWeights(Long userId) {
        var prefOpt = preferenceRepository.findByUserId(userId);
        if (prefOpt.isEmpty()) return;
        UserPreference pref = prefOpt.get();

        List<Object[]> modeStats = feedbackRepository.findMostChosenModesByUserId(userId);
        if (modeStats.isEmpty()) return;

        long helpful = feedbackRepository.countByUserIdAndRecommendationHelpful(userId, true);
        long notHelpful = feedbackRepository.countByUserIdAndRecommendationHelpful(userId, false);
        double helpfulRate = (helpful + notHelpful) > 0 ? (double) helpful / (helpful + notHelpful) : 0.5;

        if (helpfulRate < 0.6) {
            historyRepository.save(PreferenceHistory.builder()
                    .user(User.builder().id(userId).build())
                    .costWeight(pref.getCostWeight()).timeWeight(pref.getTimeWeight())
                    .comfortWeight(pref.getComfortWeight()).safetyWeight(pref.getSafetyWeight())
                    .ecoWeight(pref.getEcoWeight())
                    .triggerReason("Auto-ajuste: taxa de acerto baixa (" + Math.round(helpfulRate * 100) + "%)")
                    .version(pref.getVersion()).build());

            pref.setCostWeight(Math.max(0.05, pref.getCostWeight() + adjustmentRate));
            pref.setTimeWeight(Math.max(0.05, pref.getTimeWeight() + adjustmentRate));
            normalizeWeights(pref);
            pref.setVersion(pref.getVersion() + 1);
            preferenceRepository.save(pref);
            log.info("Pesos ajustados para usuario {} (v{})", userId, pref.getVersion());
        }
    }

    private void normalizeWeights(UserPreference pref) {
        double total = pref.getCostWeight() + pref.getTimeWeight() + pref.getComfortWeight()
                + pref.getSafetyWeight() + pref.getEcoWeight();
        if (total > 0) {
            pref.setCostWeight(pref.getCostWeight() / total);
            pref.setTimeWeight(pref.getTimeWeight() / total);
            pref.setComfortWeight(pref.getComfortWeight() / total);
            pref.setSafetyWeight(pref.getSafetyWeight() / total);
            pref.setEcoWeight(pref.getEcoWeight() / total);
        }
    }
}
