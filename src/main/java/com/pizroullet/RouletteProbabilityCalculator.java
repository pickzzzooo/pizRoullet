package com.pizroullet;

import java.util.EnumMap;
import java.util.Map;

public class RouletteProbabilityCalculator {

    private final RouletteConfigManager configManager;

    public RouletteProbabilityCalculator(RouletteConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * 현재 활성화된 모든 벌칙의 총 가중치 합을 구합니다.
     */
    public int getTotalWeight() {
        int total = 0;
        for (RewardType type : RewardType.values()) {
            total += configManager.getWeight(type);
        }
        return total;
    }

    /**
     * 특정 벌칙의 개별 당첨 확률을 백분율(%) 수치로 가져옵니다.
     * 총 가중치가 0이거나 해당 벌칙의 가중치가 0이면 0.0을 반환합니다.
     */
    public double getIndividualPercentage(RewardType type) {
        int totalWeight = getTotalWeight();
        if (totalWeight <= 0) {
            return 0.0;
        }

        int weight = configManager.getWeight(type);
        if (weight <= 0) {
            return 0.0;
        }

        // 소수점 연산을 위해 double 형변환 후 백분율 계산
        double percentage = ((double) weight / totalWeight) * 100.0;

        // 소수점 아래 첫째 자리까지 반올림 (예: 12.345 -> 12.3)
        return Math.round(percentage * 10.0) / 10.0;
    }

    /**
     * 전체 벌칙의 확률 목록을 한 번에 Map 구조로 가져옵니다.
     */
    public Map<RewardType, Double> getAllPercentages() {
        Map<RewardType, Double> percentageMap = new EnumMap<>(RewardType.class);
        for (RewardType type : RewardType.values()) {
            percentageMap.put(type, getIndividualPercentage(type));
        }
        return percentageMap;
    }
}