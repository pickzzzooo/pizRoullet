package com.pizroullet;

import org.bukkit.plugin.java.JavaPlugin;

public class PizRoullet extends JavaPlugin {

    // 1. static 플러그인 인스턴스 변수 선언
    private static PizRoullet instance;
    private RouletteConfigManager configManager;

    @Override
    public void onEnable() {
        // 2. 서버가 켜질 때 instance 할당
        instance = this;

        saveDefaultConfig();
        this.configManager = new RouletteConfigManager(this);
        this.configManager.loadWeights();
        
        // 다룰 명령어 등록
        if (getCommand("다룰") != null) {
            getCommand("다룰").setExecutor(new RouletteCommand());
        } else {
            getLogger().warning("plugin.yml에 '다룰' 명령어가 정의되지 않았습니다!");
        }

        getServer().getPluginManager().registerEvents(new RouletteGuiListener(), this);
    }

    @Override
    public void onDisable() {
        // 플러그인 종료 시 참조 해제 (메모리 누수 방지)
        instance = null;
    }

    // 3. 외부 클래스에서 호출할 수 있는 public static getInstance() 메서드 구현
    public static PizRoullet getInstance() {
        return instance;
    }

    // 4. RouletteConfigManager getter 메서드
    public RouletteConfigManager getConfigManager() {
        return configManager;
    }
}