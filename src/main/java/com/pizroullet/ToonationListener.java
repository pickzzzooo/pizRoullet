package com.pizroullet;

import com.piz.ToonationDonationEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ToonationListener implements Listener {

    @EventHandler
    public void onToonationDonation(ToonationDonationEvent event) {
        if (!RouletteManager.isRunning()) {
            return;
        }

        String nickname = event.getNickname();
        long amount = event.getAmount();
        String message = event.getMessage();

        Bukkit.broadcastMessage("§e[Toonation] §f" + nickname + "님이 §a" + amount + "원§f을 후원하셨습니다!");

        // 룰렛 가동 로직 연동
        processRouletteReward(nickname, amount, message);
    }

    private void processRouletteReward(String nickname, long amount, String message) {
        // 금액별 벌칙 룰렛 구동 로직 위치
        if (amount >= 1000) {
            Bukkit.broadcastMessage("§6[룰렛] §e후원으로 인해 벌칙 룰렛이 가동됩니다!");
            // TODO: 여기에 실제 벌칙 추첨 로직 연결
        }
    }
}