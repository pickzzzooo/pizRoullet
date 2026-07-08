package com.pizroullet;

public enum RewardType {
    DIAMOND_INCREASE("diamond_increase", "다이아 증가"),
    MONSTER_SPAWN("monster_spawn", "몬스터 소환"),
    LAVA_PLACE("lava_place", "용암 설치"),
    DROP_ALL("drop_all", "다 버리기"),
    RESIZE_PLAYER("resize_player", "크기 조정"),
    RANDOM_TELEPORT("random_teleport", "랜덤 텔포"),
    TNT_SPAWN("tnt_spawn", "tnt 소환"),
    SAND_PRISON("sand_prison", "모래 감옥"),
    COBWEB_PRISON("cobweb_prison", "거미줄 감옥"),
    WATER_CLUTCH("water_clutch", "물낙 미션"),
    JUMP_MAP("jump_map", "점프맵");

    private final String configKey;
    private final String displayName;

    RewardType(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * configKey를 기반으로 Enum 상수를 찾는 유틸리티 메서드
     */
    public static RewardType fromConfigKey(String key) {
        for (RewardType type : values()) {
            if (type.getConfigKey().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}