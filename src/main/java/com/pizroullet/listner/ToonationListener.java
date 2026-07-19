package com.pizroullet.listner;

import com.piz.ToonationDonationEvent;
import com.pizroullet.PizRoullet;
import com.pizroullet.manager.RouletteConfigManager;
import com.pizroullet.manager.RouletteManager;
import com.pizroullet.manager.RoulettePenaltyManager;
import com.pizroullet.util.RewardType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ToonationListener implements Listener {

    private static final Queue<DonationData> donationQueue = new ConcurrentLinkedQueue<>();
    private static boolean isProcessing = false;

    private static class DonationData {
        final String nickname;
        final RewardType forcedReward; // null이면 정상 가중치 추첨, 지정되면 강제 확정

        DonationData(String nickname, RewardType forcedReward) {
            this.nickname = nickname;
            this.forcedReward = forcedReward;
        }
    }

    @EventHandler
    public void onToonationDonation(ToonationDonationEvent event) {
        // 게임이 진행 중이 아니면 후원 무시
        if (!RouletteManager.isRunning()) {
            return;
        }

        String nickname = event.getNickname();
        long amount = event.getAmount();

        Bukkit.broadcastMessage("§e[Toonation] §f" + nickname + "님이 §a" + amount + "원§f을 후원하셨습니다!");

        RouletteConfigManager configManager = PizRoullet.getInstance().getConfigManager();

        // 1. 우선 이 금액에 고정으로 지정된 벌칙이 있는지 확인 (맵 구조라 무제한 등록 가능)
        RewardType fixedReward = configManager.getFixedReward((int) amount);

        if (fixedReward != null) {
            // [변경] 룰렛 애니메이션 없이 즉시 벌칙 실행!
            Bukkit.broadcastMessage("§c§l[지정 후원] §e" + nickname + "§f님의 지정 금액 후원으로 §4§l" + fixedReward.getDisplayName() + "§f 벌칙이 즉시 발동합니다!");

            // 마인크래프트 안전을 위해 메인 스레드에서 즉시 실행 보장
            Bukkit.getScheduler().runTask(PizRoullet.getInstance(), () -> {
                RoulettePenaltyManager.executeReward(fixedReward);
            });
        }
        // 2. 지정 금액 벌칙은 없지만, 일반 룰렛 최소 후원 금액 조건 충족 시
        else if (amount >= configManager.getMinDonationAmount()) {
            // 기존과 동일하게 룰렛을 돌리기 위해 대기 큐에 삽입 (forcedReward는 null)
            donationQueue.add(new DonationData(nickname, null));
            checkAndProcessNext();
        }
    }

    /**
     * 명령어가 들어오는 즉시 메인 스레드에서 해당 벌칙 기능을 다이렉트로 실행합니다.
     */
    public static void triggerTestRoulette(String senderName, RewardType forceReward) {
        if (forceReward == null) return;

        // 버킷 인게임 액션 안전성을 위해 메인 스레드에서 즉시 실행
        org.bukkit.Bukkit.getScheduler().runTask(com.pizroullet.PizRoullet.getInstance(), () -> {
            com.pizroullet.manager.RoulettePenaltyManager.executeReward(forceReward);
        });
    }

    private static void checkAndProcessNext() {
        if (isProcessing || donationQueue.isEmpty()) {
            return;
        }

        isProcessing = true;

        DonationData data = donationQueue.poll();
        if (data == null) {
            isProcessing = false;
            return;
        }

        RewardType finalReward;

        // [핵심 변경] data.forcedReward가 존재하면 룰렛 연출 후 '확정 벌칙'이 발동되도록 설정
        if (data.forcedReward != null) {
            finalReward = data.forcedReward;
        } else {
            // 실제 도네이션 유입 시 일반 추첨 엔진 가동 (이전과 동일)
            if (!RouletteManager.isRunning()) {
                isProcessing = false;
                return;
            }
            // 기존 확률 계산기 연동 (null 방어 코드 포함)
            finalReward = PizRoullet.getInstance().getProbabilityCalculator().drawReward();
            if (finalReward == null) {
                RewardType[] allTypes = RewardType.values();
                finalReward = allTypes[(int) (Math.random() * allTypes.length)];
            }
        }

        final RewardType chosenReward = finalReward;

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 20; // 룰렛 셔플 타임 = 2초
            final RewardType[] allRewards = RewardType.values();

            @Override
            public void run() {
                if (data.forcedReward == null && !RouletteManager.isRunning()) {
                    donationQueue.clear();
                    isProcessing = false;
                    this.cancel();
                    return;
                }

                if (ticks < maxTicks) {
                    RewardType shuffleType = allRewards[(int) (Math.random() * allRewards.length)];
                    String rollerMsg = "§f[ §e" + shuffleType.getDisplayName() + " §f]";

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(rollerMsg));
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                    }
                } else {
                    this.cancel();

                    String finalDisplay = chosenReward.getDisplayName();
                    Bukkit.broadcastMessage("§d§l[룰렛 결과] §e" + data.nickname + "§f님의 룰렛 결과 §c§l" + finalDisplay + "§f 당첨!");

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§c§l 당첨 벌칙: " + finalDisplay));
                        p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
                    }

                    // 인게임 아이템에 영향을 주는 벌칙 로직 메인 스레드 실행
                    Bukkit.getScheduler().runTask(PizRoullet.getInstance(), () -> {
                        RoulettePenaltyManager.executeReward(chosenReward);
                    });

                    long nextDelayTicks = 100L; // 평소 일반 벌칙일 때 대기 시간: 5초 (60틱)

                    if (chosenReward.getConfigKey().equalsIgnoreCase("SKYDIVING") || chosenReward.name().contains("SKY")) {
                        nextDelayTicks = 300L; // 스카이다이빙인 경우에만 정확히 10초 (200틱) 대기
                    }

// 계산된 동적 지연 시간(nextDelayTicks)을 대입하여 다음 큐 실행
                    Bukkit.getScheduler().runTaskLater(PizRoullet.getInstance(), () -> {
                        isProcessing = false;
                        checkAndProcessNext();
                    }, nextDelayTicks);
                }
                ticks++;
            }
        }.runTaskTimer(PizRoullet.getInstance(), 0L, 2L);
    }

    public static int getQueueSize() {
        return donationQueue.size();
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        // 플레이어가 접속하면 현재 다이아 룰렛이 켜져 있는지 확인하고 스코어보드를 장착시킵니다.
        Player player = event.getPlayer();
        com.pizroullet.manager.RouletteManager.applyScoreboardToPlayer(player);
    }
}