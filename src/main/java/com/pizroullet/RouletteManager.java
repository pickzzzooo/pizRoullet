package com.pizroullet;

public class RouletteManager {

    private static boolean isGameStarted = false;
    private static int targetDiamonds = 0;
    private static int currentDiamonds = 0;

    // 게임 시작
    public static void startGame(int target) {
        isGameStarted = true;
        targetDiamonds = target;
        currentDiamonds = 0;
    }

    // 게임 종료
    public static void stopGame() {
        isGameStarted = false;
    }

    // 현재 게임이 진행 중인지 확인하는 메서드
    public static boolean isRunning() {
        return isGameStarted;
    }

    // 다이아몬드 추가 및 승리 체크
    public static void addDiamond(int amount) {
        if (!isGameStarted) return;

        currentDiamonds += amount;
        if (currentDiamonds >= targetDiamonds) {
            // 승리 처리 로직 호출 공간
            stopGame();
        }
    }

    public static int getTargetDiamonds() { return targetDiamonds; }
    public static int getCurrentDiamonds() { return currentDiamonds; }
}