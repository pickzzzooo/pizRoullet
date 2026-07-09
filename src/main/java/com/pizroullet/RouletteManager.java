package com.pizroullet;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

public class RouletteManager {

    private static boolean isGameStarted = false;
    private static int targetDiamonds = 0;
    private static int currentDiamonds = 0;

    // 타이머 및 스코어보드 관리를 위한 변수
    private static long startTime = 0;
    private static BukkitTask timerTask = null;
    private static Scoreboard rouletteScoreboard = null;

    public static void startGame(int target) {
        isGameStarted = true;
        targetDiamonds = target;
        currentDiamonds = 0;
        startTime = System.currentTimeMillis();

        // 1. 전용 스코어보드 초기화
        initScoreboard();

        // 2. 1초(20틱)마다 스코어보드를 실시간 갱신하는 스케줄러 시작
        if (timerTask != null) timerTask.cancel();
        timerTask = Bukkit.getScheduler().runTaskTimer(PizRoullet.getInstance(), () -> {
            if (!isGameStarted) return;
            updateScoreboard();
        }, 0L, 20L);

        // 시작 알림 및 효과음
        Bukkit.broadcastMessage("§a§l[다이아 룰렛] §f새로운 룰렛 게임이 시작되었습니다! §7(목표: §b" + target + "개§7)");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(rouletteScoreboard); // 모든 플레이어 화면에 스코어보드 장착
            p.sendTitle("§a§l룰렛 시작!", "§f목표 다이아: §b" + target + "개", 10, 70, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    public static void stopGame() {
        isGameStarted = false;

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
     * 스코어보드 뼈대를 생성합니다.
     */
    private static void initScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        rouletteScoreboard = manager.getNewScoreboard();
        // 우측 사이드바 영역에 '다이아룰렛'이라는 타이틀의 오브젝티브 생성
        Objective objective = rouletteScoreboard.registerNewObjective("roulette_side", Criteria.DUMMY, "=== §b§l다이아룰렛 §f===");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    /**
     * 시간과 다이아 현황을 계산하여 스코어보드를 완전히 새로고침합니다.
     */
    /**
     * 지정된 템플릿 양식에 맞추어 스코어보드를 완전히 새로고침합니다.
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

        // 6층: 한 줄 공백
        Score score6 = objective.getScore("§1 ");
        score6.setScore(6);

        // 5층: 진행 시간 출력
        Score score5 = objective.getScore("§f진행시간 : §a" + timeStr);
        score5.setScore(5);

        // 4층: 한 줄 공백
        Score score4 = objective.getScore("§2 ");
        score4.setScore(4);

        // 3층: 다이아몬드 수량 현황 출력
        Score score3 = objective.getScore("§f다이아 : §b" + currentDiamonds + " §f/ §b" + targetDiamonds);
        score3.setScore(3);

        // 2층: 한 줄 공백
        Score score2 = objective.getScore("§3 ");
        score2.setScore(2);

        // 1층: 하단 구분선 (상단과 중복 처리를 피하기 위해 §8 색상코드 사용)
        Score score1 = objective.getScore("§f ==============");
        score1.setScore(1);
    }

    public static boolean isRunning() { return isGameStarted; }
    public static int getTargetDiamonds() { return targetDiamonds; }
    public static int getCurrentDiamonds() { return currentDiamonds; }
}