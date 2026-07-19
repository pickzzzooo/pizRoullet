package com.pizroullet.util;

import com.pizroullet.manager.RouletteConfigManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RouletteProbabilityCalculator {

    private final RouletteConfigManager configManager;

    // [신규] 생성된 벌칙 카드들을 담아두고 하나씩 꺼내 쓸 덱(Deck) 가챠 박스
    private final List<RewardType> itemDeck = new ArrayList<>();

    // 랜덤 카드 비율 설정 (기본값 25%)
    private static final double RANDOM_CARD_RATIO = 0.25;
    // 전체 덱 기준 크기 (가중치 기반 카드들이 딱 떨어지게 들어갈 수 있도록 100장 기준 세팅)
    private static final int BASE_DECK_SIZE = 100;

    public RouletteProbabilityCalculator(RouletteConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * 가중치 정보와 랜덤 카드를 조합하여 덱을 새로 만들고 섞습니다.
     */
    private void refillAndShuffleDeck() {
        itemDeck.clear();

        int totalWeight = getTotalWeight();
        if (totalWeight <= 0) return;

        // 1. 활성화된 벌칙 유형 수집
        List<RewardType> activeTypes = new ArrayList<>();
        for (RewardType type : RewardType.values()) {
            if (configManager.getWeight(type) > 0) {
                activeTypes.add(type);
            }
        }
        if (activeTypes.isEmpty()) return;

        // 2. 전체 덱 크기 중 가중치/랜덤 영역 카드 장수 계산
        int randomCardCount = (int) (BASE_DECK_SIZE * RANDOM_CARD_RATIO); // 25장
        int weightCardCount = BASE_DECK_SIZE - randomCardCount;           // 75장

        // 3. 75장의 카드 공간에 가중치 비율대로 벌칙 분배해서 채우기
        for (RewardType type : activeTypes) {
            int weight = configManager.getWeight(type);
            // 해당 벌칙이 가져갈 카드 장수 비율 계산
            int cardAllocation = (int) Math.round(((double) weight / totalWeight) * weightCardCount);

            for (int i = 0; i < cardAllocation; i++) {
                itemDeck.add(type);
            }
        }

        // 4. 나머지 25장의 공간도 기존 가중치 추첨 알고리즘을 사용하여 비율대로 랜덤 추출
        for (int i = 0; i < randomCardCount; i++) {
            int randomIndex = (int) (Math.random() * totalWeight);
            int currentSum = 0;
            for (RewardType type : activeTypes) {
                currentSum += configManager.getWeight(type);
                if (randomIndex < currentSum) {
                    itemDeck.add(type);
                    break;
                }
            }
        }

        // 5. 안전장치: 반올림 오차로 인해 100장이 살짝 안 채워졌다면 빈 곳을 랜덤하게 더 채움
        while (itemDeck.size() < BASE_DECK_SIZE) {
            RewardType fallbackPick = activeTypes.get((int) (Math.random() * activeTypes.size()));
            itemDeck.add(fallbackPick);
        }

        // 6. 완성된 가챠 박스 덱을 완전히 무작위로 뒤흔들기 (셔플)
        Collections.shuffle(itemDeck);
    }

    /**
     * [핵심 변경] 완전 운빨 추첨 대신, 준비된 덱에서 한 장을 뽑아 반환합니다.
     * 덱이 비어있거나 처음 실행 시 자동으로 덱을 생성합니다.
     */
    public RewardType drawReward() {
        // 덱이 다 떨어졌거나 비어있다면 새로 만들어서 채우기
        if (itemDeck.isEmpty()) {
            refillAndShuffleDeck();
        }

        // 덱을 생성했음에도 비어있다면 (모든 가중치가 0인 경우) 방어 코드
        if (itemDeck.isEmpty()) {
            return null;
        }

        // 덱의 가장 맨 앞(0번인덱스) 카드를 뽑아서 지급하고 덱에서 삭제 (소모성 구조)
        return itemDeck.remove(0);
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
     */
    public double getIndividualPercentage(RewardType type) {
        int totalWeight = getTotalWeight();
        if (totalWeight <= 0) return 0.0;

        int weight = configManager.getWeight(type);
        if (weight <= 0) return 0.0;

        double percentage = ((double) weight / totalWeight) * 100.0;
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

    /**
     * [추가 팁] 현재 가챠 박스 덱에 남은 카드 장수를 리턴하는 API (필요 시 활용)
     */
    public int getRemainingDeckSize() {
        return itemDeck.size();
    }
}