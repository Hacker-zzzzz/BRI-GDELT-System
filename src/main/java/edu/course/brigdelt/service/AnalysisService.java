package edu.course.brigdelt.service;

import edu.course.brigdelt.domain.CooperationScore;
import edu.course.brigdelt.domain.RiskAssessment;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;

import java.util.Comparator;
import java.util.List;

public class AnalysisService {

    public static final int DEFAULT_RANK_LIMIT = 20;

    private final GdeltEventRepository eventRepository;

    public AnalysisService(DatabaseManager databaseManager) {
        this.eventRepository = new GdeltEventRepository(databaseManager);
    }

    public List<CooperationScore> cooperationRankings(int limit) {
        return eventRepository.queryCooperationScores(effectiveLimit(limit)).stream()
                .sorted(Comparator.comparingDouble(CooperationScore::cooperationIndex).reversed())
                .toList();
    }

    public List<RiskAssessment> riskRankings(int limit) {
        return eventRepository.queryRiskAssessments(effectiveLimit(limit)).stream()
                .sorted(Comparator.comparingDouble(RiskAssessment::riskIndex).reversed())
                .toList();
    }

    private int effectiveLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_RANK_LIMIT;
        }
        return Math.min(limit, 100);
    }
}
