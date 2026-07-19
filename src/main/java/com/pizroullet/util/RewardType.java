package com.pizroullet.util;

import com.pizroullet.listner.RouletteItemListener;

public enum RewardType {
    // 1단계
    MONSTER_BUNDLE_1("monster_bundle_1", "몬스터 보따리 1", RouletteTier.TIER_1),
    DIAMOND_INCREASE("diamond_increase", "다이아 증가", RouletteTier.TIER_1),
    DEBUFF("debuff", "디버프", RouletteTier.TIER_1),
    RESIZE_SMALL("resize_small", "작아지기", RouletteTier.TIER_1),
    DROP_ALL("drop_all", "다버리기", RouletteTier.TIER_1),
    COBWEB_PRISON("cobweb_prison", "거미줄 감옥", RouletteTier.TIER_1),

    // 2단계
    MONSTER_BUNDLE_2("monster_bundle_2", "몬스터 보따리 2", RouletteTier.TIER_2),
    ZOMBIE_RAID("zombie_raid", "좀비 습격", RouletteTier.TIER_2),
    SILVERFISH_RAID("silverfish_raid", "좀벌레 습격", RouletteTier.TIER_2),
    SAND_PRISON("sand_prison", "모래 감옥", RouletteTier.TIER_2),
    TNT_SPAWN("tnt_spawn", "tnt 소환", RouletteTier.TIER_2),
    RANDOM_TELEPORT("random_teleport", "랜덤텔포", RouletteTier.TIER_2),
    LAVA_SPAWN("lava_spawn", "용암 설치", RouletteTier.TIER_2),

    // 3단계
    MONSTER_BUNDLE_3("monster_bundle_3", "몬스터 보따리 3", RouletteTier.TIER_3),
    JUMP_MAP_1("jump_map_1", "점프맵 1", RouletteTier.TIER_3),
    SKYDIVING_1("skydiving_1", "스카이다이빙 1", RouletteTier.TIER_3),

    // 4단계
    MONSTER_BUNDLE_4("monster_bundle_4", "몬스터 보따리 4", RouletteTier.TIER_4),
    JUMP_MAP_2("jump_map_2", "점프맵 2", RouletteTier.TIER_4),
    SKYDIVING_2("skydiving_2", "스카이다이빙 2", RouletteTier.TIER_4),
    INSTANT_DIE("instant_die", "즉사", RouletteTier.TIER_4);

    private final String configKey;
    private final String displayName;
    private final RouletteTier tier;

    RewardType(String configKey, String displayName, RouletteTier tier) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.tier = tier;
    }

    public String getConfigKey() { return configKey; }
    public String getDisplayName() { return displayName; }
    public RouletteTier getTier() { return tier; }

    public static RewardType fromConfigKey(String key) {
        for (RewardType type : values()) {
            if (type.getConfigKey().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}