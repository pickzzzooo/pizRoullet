package com.pizroullet.manager;

import com.pizroullet.PizRoullet;
import com.pizroullet.util.RewardType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RouletteConfigManager {

    private final Map<RewardType, Integer> rewardWeights = new EnumMap<>(RewardType.class);
    private final Map<Integer, RewardType> fixedRewards = new HashMap<>();
    private final Random random = new Random();

    private final PizRoullet plugin;
    private int minDonationAmount = 1000; // 기본값 1000원

    public RouletteConfigManager(PizRoullet plugin) {
        this.plugin = plugin;
    }

    public void loadWeights() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // 최소 후원 금액 로드
        this.minDonationAmount = config.getInt("min-donation-amount", 1000);

        // 1. 기존 룰렛 가중치 로드
        for (RewardType type : RewardType.values()) {
            String path = "roulette-rewards." + type.getConfigKey();

            if (!config.contains(path)) {
                config.set(path, 10);
            }

            int weight = config.getInt(path, 10);
            rewardWeights.put(type, weight);
        }

        // 2. [추가] 지정 금액 확정 벌칙 설정 로드
        fixedRewards.clear();
        ConfigurationSection fixedSection = config.getConfigurationSection("fixed-donation-rewards");
        if (fixedSection != null) {
            for (String keyStr : fixedSection.getKeys(false)) {
                try {
                    int amount = Integer.parseInt(keyStr);
                    String rewardKey = fixedSection.getString(keyStr);

                    // RewardType의 configKey와 일치하는 타입을 찾아 매핑
                    RewardType matchedType = fromConfigKey(rewardKey);
                    if (matchedType != null) {
                        fixedRewards.put(amount, matchedType);
                    } else {
                        plugin.getLogger().warning("지정 금액 벌칙 로드 실패: '" + rewardKey + "'는 올바른 벌칙 키가 아닙니다.");
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("지정 금액 벌칙 로드 실패: 금액 설정('" + keyStr + "')이 올바른 정수가 아닙니다.");
                }
            }
        }

        // 새로 추가/갱신된 기본값 항목들이 있다면 파일에 영구 저장
        plugin.saveConfig();
        plugin.getLogger().info("룰렛 설정, 가중치 및 지정 금액 벌칙 로드가 완료되었습니다.");
    }

    /**
     * [추가] 후원 금액에 따른 벌칙을 최종 판별하여 반환합니다.
     * @param donationAmount 실제 후원된 금액
     * @return 당첨되거나 지정된 벌칙 타입 (진행 조건 미달 시 null)
     */
    public RewardType determineReward(int donationAmount) {
        // 1. 지정 금액 확정 벌칙이 있는지 먼저 확인
        if (fixedRewards.containsKey(donationAmount)) {
            return fixedRewards.get(donationAmount);
        }

        // 2. 확정 벌칙이 없다면 기존대로 최소 후원 금액 검사 후 룰렛 돌리기
        if (donationAmount >= minDonationAmount) {
            return rollRoulette();
        }

        // 최소 금액 미만이면 아무것도 발동하지 않음
        return null;
    }

    /**
     * 누적 가중치(정수 표)를 계산하여 무작위로 벌칙 하나를 추첨합니다.
     */
    public RewardType rollRoulette() {
        int totalWeight = 0;

        for (int weight : rewardWeights.values()) {
            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            return null;
        }

        int randomIndex = random.nextInt(totalWeight);
        int currentSum = 0;

        for (Map.Entry<RewardType, Integer> entry : rewardWeights.entrySet()) {
            currentSum += entry.getValue();
            if (randomIndex < currentSum) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * [추가] 인게임 GUI 등에서 특정 금액에 벌칙을 확정 매핑하고 파일에 물리 저장할 때 사용
     */
    public void setAndSaveFixedReward(int amount, RewardType type) {
        fixedRewards.put(amount, type);
        plugin.getConfig().set("fixed-donation-rewards." + amount, type.getConfigKey());
        plugin.saveConfig();
        plugin.getLogger().info(amount + "원 후원 시 " + type.getDisplayName() + " 벌칙이 확정 발동되도록 물리 저장되었습니다.");
    }

    /**
     * [추가] configKey 문자열로 RewardType을 역추적하는 헬퍼 메서드
     */
    private RewardType fromConfigKey(String configKey) {
        for (RewardType type : RewardType.values()) {
            if (type.getConfigKey().equalsIgnoreCase(configKey)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 특정 금액에 매핑된 고정 벌칙을 가져오는 Getter
     */
    public RewardType getFixedReward(int amount) {
        return fixedRewards.get(amount);
    }

    public int getMinDonationAmount() { return minDonationAmount; }

    public void setAndSaveMinDonationAmount(int amount) {
        this.minDonationAmount = amount;
        plugin.getConfig().set("min-donation-amount", amount);
        plugin.saveConfig();
    }

    public void setAndSaveWeight(RewardType type, int newWeight) {
        rewardWeights.put(type, newWeight);
        FileConfiguration config = plugin.getConfig();
        config.set("roulette-rewards." + type.getConfigKey(), newWeight);
        plugin.saveConfig();
        plugin.getLogger().info(type.getDisplayName() + "의 가중치가 변경 및 물리 저장되었습니다.");
    }

    public int getWeight(RewardType type) {
        return rewardWeights.getOrDefault(type, 0);
    }
}