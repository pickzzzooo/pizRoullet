package com.pizroullet.util;

import org.bukkit.Sound;

public enum RouletteTier {
    TIER_1("§a", Sound.ENTITY_ITEM_BREAK),       // 1단계: 초록색, 도구 부러지는 소리
    TIER_2("§9", Sound.ENTITY_GENERIC_EXPLODE),  // 2단계: 파란색, TNT 터지는 소리
    TIER_3("§c", Sound.ENTITY_LIGHTNING_BOLT_THUNDER), // 3단계: 빨간색, 번개 소리
    TIER_4("§4", Sound.ENTITY_ENDER_DRAGON_GROWL); // 4단계: 진한 빨간색, 드래곤 울음소리

    private final String colorCode;
    private final Sound sound;

    RouletteTier(String colorCode, Sound sound) {
        this.colorCode = colorCode;
        this.sound = sound;
    }

    public String getColorCode() {
        return this.colorCode;
    }

    public Sound getSound() {
        return this.sound;
    }
}