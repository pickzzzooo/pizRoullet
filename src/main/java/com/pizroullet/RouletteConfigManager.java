package com.pizroullet;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class RouletteConfigManager {

    private final JavaPlugin plugin;
    private final Map<RewardType, Integer> rewardWeights = new EnumMap<>(RewardType.class);
    private final Random random = new Random();

    public RouletteConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * config.yml 파일로부터 가중치 데이터를 메모리에 로드합니다.
     */
    public void loadWeights() {
        rewardWeights.clear();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        ConfigurationSection section = config.getConfigurationSection("roulette-rewards");
        if (section == null) {
            plugin.getLogger().warning("config.yml에서 'roulette-rewards' 섹션을 찾을 수 없습니다. 기본값을 생성하거나 확인해주세요.");
            return;
        }

        // Enum에 정의된 모든 벌칙에 대해 가중치를 읽어옴 (없으면 기본값 0)
        for (RewardType type : RewardType.values()) {
            int weight = section.getInt(type.getConfigKey(), 0);
            // 가중치가 음수일 경우 0으로 보정
            if (weight < 0) weight = 0;

            rewardWeights.put(type, weight);
        }
        plugin.getLogger().info("룰렛 벌칙 가중치 로드가 완료되었습니다.");
    }

    /**
     * 특정 벌칙의 가중치를 인게임에서 변경하고 config.yml 파일에 실시간으로 저장 및 메모리 동기화를 수행합니다.
     */
    public void setAndSaveWeight(RewardType type, int newWeight) {
        // 1. 메모리 상의 맵 데이터 즉시 업데이트
        rewardWeights.put(type, newWeight);

        // 2. 플러그인 내부 FileConfiguration 객체의 roulette-rewards 섹션 하위에 값 수정 반영
        FileConfiguration config = plugin.getConfig();
        config.set("roulette-rewards." + type.getConfigKey(), newWeight);

        // 3. 수정된 설정을 디스크(config.yml 물리 파일)에 영구 저장
        plugin.saveConfig();
        plugin.getLogger().info(type.getDisplayName() + "의 가중치가 인게임 GUI 설정에 의해 " + newWeight + "(으)로 변경 및 물리 저장되었습니다.");
    }

    /**
     * 누적 가중치(정수 표)를 계산하여 무작위로 벌칙 하나를 추첨합니다.
     * @return 추첨된 벌칙 타입, 활성화된 벌칙이 없다면 null 리턴
     */
    public RewardType rollRoulette() {
        int totalWeight = 0;

        // 1. 활성화된(가중치 > 0) 벌칙들의 총 가중치 합산
        for (int weight : rewardWeights.values()) {
            totalWeight += weight;
        }

        // 모든 벌칙이 0(Off)이거나 설정된 벌칙이 없다면 null
        if (totalWeight <= 0) {
            return null;
        }

        // 2. 0부터 (총 가중치 합 - 1) 사이의 난수 생성
        int randomIndex = random.nextInt(totalWeight);
        int currentSum = 0;

        // 3. 정수 값만큼 순차적으로 더해가며 당첨 구간 확인 (A:1, B:3 구조 구현)
        for (Map.Entry<RewardType, Integer> entry : rewardWeights.entrySet()) {
            currentSum += entry.getValue();
            if (randomIndex < currentSum) {
                return entry.getKey(); // 당첨된 벌칙 반환
            }
        }

        return null;
    }

    /**
     * 특정 벌칙의 현재 가중치를 가져옵니다.
     */
    public int getWeight(RewardType type) {
        return rewardWeights.getOrDefault(type, 0);
    }
}