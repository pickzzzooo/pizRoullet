package com.pizroullet.manager;

import com.pizroullet.PizRoullet;
import com.pizroullet.listner.ToonationListener; // [추가] 리스너 패키지 임포트
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.UUID;

public class RouletteManager {

    private static boolean isGameStarted = false;
    private static int targetDiamonds = 0;
    private static int currentDiamonds = 0;

    // [추가] 다이아 룰렛 참가자의 고유 UUID 세션 보관 변수
    private static UUID participantUUID = null;

    // 타이머 및 스코어보드 관리를 위한 변수
    private static long startTime = 0;
    private static BukkitTask timerTask = null;
    private static Scoreboard rouletteScoreboard = null;
    private static boolean isParticipantDead = false; // 사망 펄스

    public static void startGame(Player initiator, int target) {
        isGameStarted = true;
        isParticipantDead = false;
        targetDiamonds = target;
        currentDiamonds = 0;
        startTime = System.currentTimeMillis();

        // [추가] 누른 사람의 UUID를 참여자로 안전하게 기록
        participantUUID = initiator.getUniqueId();

        // 1. 전용 스코어보드 초기화
        initScoreboard();

        // 2. 1초(20틱)마다 스코어보드를 실시간 갱신하는 스케줄러 시작
        if (timerTask != null) timerTask.cancel();
        timerTask = Bukkit.getScheduler().runTaskTimer(PizRoullet.getInstance(), () -> {
            if (!isGameStarted) return;
            updateScoreboard();
        }, 0L, 20L);

        Bukkit.broadcastMessage("§a§l[다이아 룰렛] §e" + initiator.getName() + "§f님이 참여자로 등록되어 새 게임이 시작되었습니다! §7(목표: §b" + target + "개§7)");

        // 참여자에게 기존 아이템을 전부 삭제하고 룰렛 전용 특수 곡괭이 지급
        if (initiator.isOnline()) {
            // 1. 인벤토리 전체 싹 비우기 (장비창, 인벤토리, 왼손 모두 초기화)
            initiator.getInventory().clear();

            // 2. 특수 곡괭이 생성 및 설정
            org.bukkit.inventory.ItemStack specialPickaxe = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_PICKAXE);
            org.bukkit.inventory.meta.ItemMeta meta = specialPickaxe.getItemMeta();

            if (meta != null) {
                // 파괴 불가 설정
                meta.setUnbreakable(true);

                // 특수 아이템 판별을 위한 고유 NBT 태그 주입
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(PizRoullet.getInstance(), "roulette_pickaxe");
                meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

                specialPickaxe.setItemMeta(meta);
            }

            // 3. 빈 인벤토리에 곡괭이 지급
            initiator.getInventory().addItem(specialPickaxe);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(rouletteScoreboard); // 모든 플레이어 화면에 스코어보드 장착
            p.sendTitle("§a§l룰렛 시작!", "§f참여자: §e" + initiator.getName() + " §7/ §f목표: §b" + target + "개", 10, 70, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    public static void stopGame() {
        isGameStarted = false;
        isParticipantDead = false;
        participantUUID = null;

        // 타이머 스케줄러 종료
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        // 모든 플레이어의 스코어보드를 기본값으로 원복하고 정리
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setScoreboard(manager.getMainScoreboard());
            }
        }

        Bukkit.broadcastMessage("§c§l[다이아 룰렛] §f진행 중이던 룰렛 게임이 종료되었습니다.");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
        }
    }

    public static void addDiamond(int amount) {
        if (!isGameStarted) return;

        currentDiamonds += amount;
        updateScoreboard(); // 다이아가 추가될 때 즉시 스코어보드 반영

        if (currentDiamonds >= targetDiamonds) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§6§l목표 달성!", "§f다이아 §b" + targetDiamonds + "개§f를 모두 모았습니다!", 10, 70, 20);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
            stopGame();
        }
    }

    /**
     * [벌칙 연동용] 목표 다이아몬드 개수를 강제로 늘립니다.
     */
    public static void increaseTargetDiamonds(int amount) {
        if (!isGameStarted) return;

        targetDiamonds += amount;
        updateScoreboard(); // 늘어난 목표치를 스코어보드에 즉시 반영
    }

    /**
     * 스코어보드 뼈대를 생성합니다.
     */
    private static void initScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        rouletteScoreboard = manager.getNewScoreboard();
        // 우측 사이드바 영역에 '다이아룰렛'이라는 타이틀의 오브젝티브 생성
        Objective objective = rouletteScoreboard.registerNewObjective("roulette_side", Criteria.DUMMY, "§b§l다이아룰렛");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    /**
     * 시간과 다이아 현황을 계산하여 스코어보드를 완전히 새로고침합니다.
     */
    private static void updateScoreboard() {
        if (rouletteScoreboard == null) return;
        Objective objective = rouletteScoreboard.getObjective("roulette_side");
        if (objective == null) return;

        // 기존에 찍혀있던 스코어보드 라인들을 전부 초기화 (누적 잔상 방지)
        for (String entry : rouletteScoreboard.getEntries()) {
            rouletteScoreboard.resetScores(entry);
        }

        // 진행 시간 계산 (hh:mm:ss)
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long h = elapsedSeconds / 3600;
        long m = (elapsedSeconds % 3600) / 60;
        long s = elapsedSeconds % 60;
        String timeStr = String.format("%02d:%02d:%02d", h, m, s);

        // 3층: 진행 시간 출력
        Score score5 = objective.getScore("§f진행시간 : §a" + timeStr);
        score5.setScore(3);


        // 2층: 다이아몬드 수량 현황 출력
        Score score3 = objective.getScore("§f다이아 : §b" + currentDiamonds + " §f/ §b" + targetDiamonds);
        score3.setScore(2);


        // [수정] 1층: 현재 후원 큐 개수 실시간 연동 출력
        int queueSize = ToonationListener.getQueueSize();
        Score score1 = objective.getScore("§f대기 중인 룰렛 : §e" + queueSize + "개");
        score1.setScore(1);
    }

    public static void applyScoreboardToPlayer(Player player) {
        if (isGameStarted && rouletteScoreboard != null) {
            player.setScoreboard(rouletteScoreboard);
        }
    }

    // [추가] 타벌칙 매니저나 리스너에서 현재 타겟 참가자를 단일 호출할 수 있는 안전한 Getter API
    public static Player getParticipant() {
        if (participantUUID == null) return null;
        return Bukkit.getPlayer(participantUUID);
    }

    public static boolean isRunning() { return isGameStarted; }
    public static int getTargetDiamonds() { return targetDiamonds; }
    public static int getCurrentDiamonds() { return currentDiamonds; }

    public static void setParticipantDead(boolean dead) { isParticipantDead = dead; }

    public static boolean isPausedByDeath() { return isGameStarted && isParticipantDead; }
}