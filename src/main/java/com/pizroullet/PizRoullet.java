package com.pizroullet;

import com.pizroullet.listner.*;
import com.pizroullet.manager.RouletteConfigManager;
import com.pizroullet.util.RouletteProbabilityCalculator;
import org.bukkit.plugin.java.JavaPlugin;

public class PizRoullet extends JavaPlugin {

    private static PizRoullet instance;
    private RouletteConfigManager configManager;
    // 계산기 변수 추가
    private RouletteProbabilityCalculator probabilityCalculator;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.configManager = new RouletteConfigManager(this);
        this.configManager.loadWeights();

        // 계산기 인스턴스 초기화
        this.probabilityCalculator = new RouletteProbabilityCalculator(this.configManager);

        // 리스너 등록
        getServer().getPluginManager().registerEvents(new RouletteGuiListener(), this);
        getServer().getPluginManager().registerEvents(new RouletteGameListener(), this);
        getServer().getPluginManager().registerEvents(new RouletteItemListener(), this);
        getServer().getPluginManager().registerEvents(new ToonationListener(), this);
        getServer().getPluginManager().registerEvents(new ChzzkListener(), this);
        
        // 명령어 등록
        RouletteCommand rouletteCommand = new RouletteCommand();
        getCommand("다룰").setExecutor(rouletteCommand);
        getCommand("다룰").setTabCompleter(rouletteCommand);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static PizRoullet getInstance() {
        return instance;
    }

    public RouletteConfigManager getConfigManager() {
        return configManager;
    }

    // 외부에서 계산기를 호출할 수 있는 Getter 추가
    public RouletteProbabilityCalculator getProbabilityCalculator() {
        return probabilityCalculator;
    }
}