package com.pizroullet.listner;

import com.piz.ChzzkDonationEvent; // 앞서 제작한 치지직 이벤트 임포트
import com.pizroullet.PizRoullet;
import com.pizroullet.manager.RouletteConfigManager;
import com.pizroullet.manager.RouletteManager;
import com.pizroullet.manager.RoulettePenaltyManager;
import com.pizroullet.util.RewardType;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChzzkListener implements Listener {

    @EventHandler
    public void onChzzkDonation(ChzzkDonationEvent event) {
        // 게임이 진행 중이 아니면 치지직 후원 무시
        if (!RouletteManager.isRunning()) {
            return;
        }

        // 닉네임 null 체크 및 치환
        String nickname = event.getNickname();
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = "익명";
        }

        long amount = event.getAmount();

        // 전체 서버에 치지직 후원 알림
        Bukkit.broadcastMessage("§b[Chzzk] §f" + nickname + "님이 §a" + amount + "원§f을 후원하셨습니다.");

        RouletteConfigManager configManager = PizRoullet.getInstance().getConfigManager();

        // 1. 해당 금액에 고정으로 지정된 벌칙이 있는지 확인
        RewardType fixedReward = configManager.getFixedReward((int) amount);

        if (fixedReward != null) {
            Bukkit.broadcastMessage("§c§l[지정 벌칙] §e" + nickname + "§f님이 §4§l" + fixedReward.getDisplayName() + "§f 를 발동했습니다.");

            // 메인 스레드에서 즉시 실행 보장
            Bukkit.getScheduler().runTask(PizRoullet.getInstance(), () -> {
                RoulettePenaltyManager.executeReward(fixedReward);
            });
        }
        // 2. 지정 금액 벌칙은 없지만, 일반 룰렛 최소 후원 금액 조건 충족 시
        else if (amount >= configManager.getMinDonationAmount()) {
            // ToonationListener의 큐와 연동 및 실행 프로세스 공유
            ToonationListener.addDonationToQueue(nickname);
        }
    }
}