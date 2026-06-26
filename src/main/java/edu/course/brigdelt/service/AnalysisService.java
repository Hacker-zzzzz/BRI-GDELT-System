package edu.course.brigdelt.service;

import edu.course.brigdelt.domain.CooperationScore;
import edu.course.brigdelt.domain.CooperationHotspot;
import edu.course.brigdelt.domain.Country;
import edu.course.brigdelt.domain.CountryClusterResult;
import edu.course.brigdelt.domain.RiskAssessment;
import edu.course.brigdelt.domain.RiskHotspot;
import edu.course.brigdelt.domain.RegionSummary;
import edu.course.brigdelt.repository.CountryRepository;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;
import edu.course.brigdelt.repository.ImportBatchRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalysisService {

    public static final int DEFAULT_RANK_LIMIT = 50;
    private static final int MAX_CACHE_ENTRIES = 32;
    private static final Map<LimitCacheKey, List<CooperationScore>> COOPERATION_CACHE = lruCache();
    private static final Map<LimitCacheKey, List<CooperationHotspot>> COOPERATION_HOTSPOT_CACHE = lruCache();
    private static final Map<LimitCacheKey, List<RiskAssessment>> RISK_CACHE = lruCache();
    private static final Map<LimitCacheKey, List<RiskHotspot>> RISK_HOTSPOT_CACHE = lruCache();
    private static final Map<CacheVersion, List<RegionSummary>> REGION_CACHE = lruCache();
    private static final Map<CacheVersion, List<CountryClusterResult>> CLUSTER_CACHE = lruCache();

    private final GdeltEventRepository eventRepository;
    private final CountryRepository countryRepository;
    private final ImportBatchRepository importBatchRepository;

    public AnalysisService(DatabaseManager databaseManager) {
        this.eventRepository = new GdeltEventRepository(databaseManager);
        this.countryRepository = new CountryRepository(databaseManager);
        this.importBatchRepository = new ImportBatchRepository(databaseManager);
    }

    /**
     * Ranks configured BRI countries by a classroom-friendly cooperation index.
     */
    public List<CooperationScore> cooperationRankings(int limit) {
        int safeLimit = effectiveLimit(limit);
        LimitCacheKey key = new LimitCacheKey(cacheVersion(), safeLimit);
        synchronized (COOPERATION_CACHE) {
            List<CooperationScore> cached = COOPERATION_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        List<CooperationScore> results = eventRepository.queryCooperationScores(safeLimit).stream()
                .sorted(Comparator.comparingDouble(CooperationScore::cooperationIndex).reversed())
                .toList();
        synchronized (COOPERATION_CACHE) {
            COOPERATION_CACHE.put(key, results);
        }
        return results;
    }

    public List<CooperationHotspot> cooperationHotspots(int limit) {
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 50);
        LimitCacheKey key = new LimitCacheKey(cacheVersion(), safeLimit);
        synchronized (COOPERATION_HOTSPOT_CACHE) {
            List<CooperationHotspot> cached = COOPERATION_HOTSPOT_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        List<CooperationHotspot> sortedHotspots = eventRepository.queryCooperationHotspots(500).stream()
                .sorted(Comparator.comparingDouble(CooperationHotspot::growth).reversed()
                        .thenComparing(Comparator.comparingInt(CooperationHotspot::cooperationEventIncrease).reversed()))
                .toList();
        List<CooperationHotspot> growingHotspots = sortedHotspots.stream()
                .filter(hotspot -> hotspot.growth() > 0 || hotspot.cooperationEventIncrease() > 0)
                .limit(safeLimit)
                .toList();
        if (!growingHotspots.isEmpty()) {
            synchronized (COOPERATION_HOTSPOT_CACHE) {
                COOPERATION_HOTSPOT_CACHE.put(key, growingHotspots);
            }
            return growingHotspots;
        }
        List<CooperationHotspot> results = sortedHotspots.stream()
                .sorted(Comparator.comparingDouble(CooperationHotspot::growth).reversed()
                        .thenComparing(Comparator.comparingInt(CooperationHotspot::cooperationEventIncrease).reversed()))
                .limit(safeLimit)
                .toList();
        synchronized (COOPERATION_HOTSPOT_CACHE) {
            COOPERATION_HOTSPOT_CACHE.put(key, results);
        }
        return results;
    }

    /**
     * Ranks configured BRI countries by conflict ratio, negative tone and negative Goldstein signals.
     */
    public List<RiskAssessment> riskRankings(int limit) {
        int safeLimit = effectiveLimit(limit);
        LimitCacheKey key = new LimitCacheKey(cacheVersion(), safeLimit);
        synchronized (RISK_CACHE) {
            List<RiskAssessment> cached = RISK_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        List<RiskAssessment> results = eventRepository.queryRiskAssessments(safeLimit).stream()
                .sorted(Comparator.comparingDouble(RiskAssessment::riskIndex).reversed())
                .toList();
        synchronized (RISK_CACHE) {
            RISK_CACHE.put(key, results);
        }
        return results;
    }

    public List<RiskHotspot> riskHotspots(int limit) {
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 50);
        LimitCacheKey key = new LimitCacheKey(cacheVersion(), safeLimit);
        synchronized (RISK_HOTSPOT_CACHE) {
            List<RiskHotspot> cached = RISK_HOTSPOT_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        List<RiskHotspot> sortedHotspots = eventRepository.queryRiskHotspots(500).stream()
                .sorted(Comparator.comparingDouble(RiskHotspot::growth).reversed()
                        .thenComparing(Comparator.comparingInt(RiskHotspot::conflictEventIncrease).reversed()))
                .toList();
        List<RiskHotspot> growingHotspots = sortedHotspots.stream()
                .filter(hotspot -> hotspot.growth() > 0 || hotspot.conflictEventIncrease() > 0)
                .limit(safeLimit)
                .toList();
        if (!growingHotspots.isEmpty()) {
            synchronized (RISK_HOTSPOT_CACHE) {
                RISK_HOTSPOT_CACHE.put(key, growingHotspots);
            }
            return growingHotspots;
        }
        List<RiskHotspot> results = sortedHotspots.stream()
                .limit(safeLimit)
                .toList();
        synchronized (RISK_HOTSPOT_CACHE) {
            RISK_HOTSPOT_CACHE.put(key, results);
        }
        return results;
    }

    /**
     * Aggregates configured BRI countries by sub-region for regional comparison and export.
     */
    public List<RegionSummary> regionSummaries() {
        CacheVersion key = cacheVersion();
        synchronized (REGION_CACHE) {
            List<RegionSummary> cached = REGION_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        List<RegionSummary> results = eventRepository.queryRegionSummaries();
        synchronized (REGION_CACHE) {
            REGION_CACHE.put(key, results);
        }
        return results;
    }

    /**
     * Runs a deterministic lightweight K-Means clustering over country-level cooperation/risk features.
     */
    public List<CountryClusterResult> countryClusters() {
        CacheVersion key = cacheVersion();
        synchronized (CLUSTER_CACHE) {
            List<CountryClusterResult> cached = CLUSTER_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        List<CooperationScore> cooperationScores = eventRepository.queryCooperationScores(1_000);
        List<RiskAssessment> riskAssessments = eventRepository.queryRiskAssessments(1_000);
        Map<String, RiskAssessment> risksByCountry = new HashMap<>();
        for (RiskAssessment risk : riskAssessments) {
            risksByCountry.put(risk.countryCode(), risk);
        }
        Map<String, Country> countriesByCode = new HashMap<>();
        for (Country country : countryRepository.findAllCountries()) {
            countriesByCode.put(country.cameoCode(), country);
        }

        List<ClusterInput> inputs = new ArrayList<>();
        int maxEvents = cooperationScores.stream().mapToInt(CooperationScore::totalEvents).max().orElse(1);
        for (CooperationScore score : cooperationScores) {
            RiskAssessment risk = risksByCountry.get(score.countryCode());
            Country country = countriesByCode.get(score.countryCode());
            if (risk == null || country == null) {
                continue;
            }
            inputs.add(new ClusterInput(score, risk, country, features(score, risk, maxEvents)));
        }
        if (inputs.isEmpty()) {
            return List.of();
        }

        int k = Math.min(4, inputs.size());
        int[] assignments = runKMeans(inputs, k);
        Map<Integer, String> labels = clusterLabels(inputs, assignments, k);
        List<CountryClusterResult> results = new ArrayList<>();
        for (int index = 0; index < inputs.size(); index++) {
            ClusterInput input = inputs.get(index);
            String label = labels.get(assignments[index]);
            results.add(new CountryClusterResult(
                    input.score().countryCode(),
                    input.country().nameCn(),
                    input.country().region(),
                    input.score().totalEvents(),
                    input.score().cooperationIndex(),
                    input.risk().riskIndex(),
                    input.risk().conflictRatio(),
                    input.score().averageGoldstein(),
                    input.score().averageAvgTone(),
                    label,
                    clusterExplanation(label)
            ));
        }
        List<CountryClusterResult> sortedResults = results.stream()
                .sorted(Comparator.comparing(CountryClusterResult::clusterLabel)
                        .thenComparing(Comparator.comparingDouble(CountryClusterResult::cooperationIndex).reversed()))
                .toList();
        synchronized (CLUSTER_CACHE) {
            CLUSTER_CACHE.put(key, sortedResults);
        }
        return sortedResults;
    }

    private int effectiveLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_RANK_LIMIT;
        }
        return Math.min(limit, 1_000);
    }

    private double[] features(CooperationScore score, RiskAssessment risk, int maxEvents) {
        return new double[]{
                score.cooperationIndex() / 100.0,
                risk.riskIndex() / 100.0,
                risk.conflictRatio(),
                normalizeGoldstein(score.averageGoldstein()),
                normalizeTone(score.averageAvgTone()),
                maxEvents <= 0 ? 0 : (double) score.totalEvents() / maxEvents
        };
    }

    private double normalizeGoldstein(double value) {
        return Math.max(0, Math.min(1, (value + 10.0) / 20.0));
    }

    private double normalizeTone(double value) {
        return Math.max(0, Math.min(1, (value + 100.0) / 200.0));
    }

    private int[] runKMeans(List<ClusterInput> inputs, int k) {
        double[][] centroids = initialCentroids(inputs, k);
        int[] assignments = new int[inputs.size()];
        for (int iteration = 0; iteration < 30; iteration++) {
            boolean changed = assign(inputs, centroids, assignments);
            recomputeCentroids(inputs, centroids, assignments, k);
            if (!changed && iteration > 0) {
                break;
            }
        }
        return assignments;
    }

    private double[][] initialCentroids(List<ClusterInput> inputs, int k) {
        List<ClusterInput> ordered = inputs.stream()
                .sorted(Comparator.comparingDouble(input -> input.risk().riskIndex() - input.score().cooperationIndex()))
                .toList();
        double[][] centroids = new double[k][];
        for (int index = 0; index < k; index++) {
            int sourceIndex = Math.min(ordered.size() - 1, (int) Math.round(index * (ordered.size() - 1.0) / Math.max(1, k - 1)));
            centroids[index] = ordered.get(sourceIndex).features().clone();
        }
        return centroids;
    }

    private boolean assign(List<ClusterInput> inputs, double[][] centroids, int[] assignments) {
        boolean changed = false;
        for (int index = 0; index < inputs.size(); index++) {
            int nearest = nearestCentroid(inputs.get(index).features(), centroids);
            if (assignments[index] != nearest) {
                assignments[index] = nearest;
                changed = true;
            }
        }
        return changed;
    }

    private int nearestCentroid(double[] features, double[][] centroids) {
        int nearest = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int index = 0; index < centroids.length; index++) {
            double distance = squaredDistance(features, centroids[index]);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = index;
            }
        }
        return nearest;
    }

    private double squaredDistance(double[] left, double[] right) {
        double sum = 0;
        for (int index = 0; index < left.length; index++) {
            double diff = left[index] - right[index];
            sum += diff * diff;
        }
        return sum;
    }

    private void recomputeCentroids(List<ClusterInput> inputs, double[][] centroids, int[] assignments, int k) {
        double[][] sums = new double[k][centroids[0].length];
        int[] counts = new int[k];
        for (int index = 0; index < inputs.size(); index++) {
            int cluster = assignments[index];
            counts[cluster]++;
            double[] features = inputs.get(index).features();
            for (int feature = 0; feature < features.length; feature++) {
                sums[cluster][feature] += features[feature];
            }
        }
        for (int cluster = 0; cluster < k; cluster++) {
            if (counts[cluster] == 0) {
                continue;
            }
            for (int feature = 0; feature < centroids[cluster].length; feature++) {
                centroids[cluster][feature] = sums[cluster][feature] / counts[cluster];
            }
        }
    }

    private Map<Integer, String> clusterLabels(List<ClusterInput> inputs, int[] assignments, int k) {
        List<ClusterProfile> profiles = new ArrayList<>();
        for (int cluster = 0; cluster < k; cluster++) {
            double cooperation = 0;
            double risk = 0;
            int count = 0;
            for (int index = 0; index < inputs.size(); index++) {
                if (assignments[index] == cluster) {
                    cooperation += inputs.get(index).score().cooperationIndex();
                    risk += inputs.get(index).risk().riskIndex();
                    count++;
                }
            }
            profiles.add(new ClusterProfile(cluster,
                    count == 0 ? 0 : cooperation / count,
                    count == 0 ? 0 : risk / count));
        }
        List<ClusterProfile> ordered = profiles.stream()
                .sorted(Comparator.comparingDouble(profile -> profile.riskIndex() - profile.cooperationIndex() * 0.35))
                .toList();
        String[] names = {"深度合作伙伴", "稳定合作", "存在风险", "高度紧张"};
        Map<Integer, String> labels = new HashMap<>();
        for (int index = 0; index < ordered.size(); index++) {
            labels.put(ordered.get(index).cluster(), names[Math.min(index, names.length - 1)]);
        }
        return labels;
    }

    private String clusterExplanation(String label) {
        return switch (label) {
            case "深度合作伙伴" -> "合作指数高、风险信号低，适合作为重点合作案例展示。";
            case "稳定合作" -> "合作基础较稳定，冲突与负面语调处于可控水平。";
            case "存在风险" -> "合作仍有基础，但冲突占比或负面信号需要关注。";
            case "高度紧张" -> "风险指数较高，适合作为风险预警和原因分析对象。";
            default -> "基于合作、冲突、语调和事件量的综合聚类结果。";
        };
    }

    private CacheVersion cacheVersion() {
        return new CacheVersion(eventRepository.countEvents(), importBatchRepository.countImportBatches());
    }

    private static <K, V> Map<K, V> lruCache() {
        return Collections.synchronizedMap(new LinkedHashMap<>(MAX_CACHE_ENTRIES, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > MAX_CACHE_ENTRIES;
            }
        });
    }

    private record CacheVersion(int eventCount, int importBatchCount) {
    }

    private record LimitCacheKey(CacheVersion version, int limit) {
    }

    private record ClusterInput(CooperationScore score, RiskAssessment risk, Country country, double[] features) {
    }

    private record ClusterProfile(int cluster, double cooperationIndex, double riskIndex) {
    }
}
