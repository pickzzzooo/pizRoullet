package com.pizroullet;

import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RouletteGuiListener implements Listener {

    public static final String SUB_GUI_TITLE = "§0다이아룰렛 - 세부 설정";
    private final RouletteConfigManager configManager = PizRoullet.getInstance().getConfigManager();

    // 6줄(54칸) 구조로 확장하여 레이아웃을 전면 재조정합니다.
    // 4번째 줄 안쪽 슬롯: 28, 29, 30, 31, 32, 33, 34 (7개)
    // 5번째 줄 안쪽 슬롯: 37, 38, 39, 40 (4개) -> 총 11개 배치
    // 이렇게 배치하면 6번째 줄(45~53번)은 완벽히 공백으로 남습니다.
    private final int[] SLOTS = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40};

    // 2번째 줄 정중앙 슬롯 번호
    private final int INFO_SLOT = 13;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (!title.equals(RouletteCommand.GUI_TITLE) && !title.equals(SUB_GUI_TITLE)) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();




        if (title.equals(RouletteCommand.GUI_TITLE)) {
            int slot = event.getSlot();

            if (slot == 11) {
                // [ 세부 설정 변경 ]
                openSubSettingsGui(player);
            }
            else if (slot == 13) {
                // [ 게임 시작 ]
                if (RouletteManager.isRunning()) {
                    player.sendMessage("§c이미 게임이 진행 중입니다!");
                    player.closeInventory();
                    return;
                }
                openTargetDiamondInputGui(player);
            }
            else if (slot == 15) {
                // [ 게임 중단 ]
                if (!RouletteManager.isRunning()) {
                    player.sendMessage("§c현재 진행 중인 게임이 없습니다!");
                    player.closeInventory();
                    return;
                }
                RouletteManager.stopGame();
                player.closeInventory();
            }
        }

        else if (title.equals(SUB_GUI_TITLE)) {
            int clickedSlot = event.getSlot();
            RewardType[] types = RewardType.values();

            int matchedIndex = -1;
            for (int i = 0; i < SLOTS.length; i++) {
                if (SLOTS[i] == clickedSlot) {
                    matchedIndex = i;
                    break;
                }
            }

            if (matchedIndex >= 0 && matchedIndex < types.length) {
                RewardType selectedType = types[matchedIndex];
                int currentWeight = configManager.getWeight(selectedType);

                openAnvilInputGui(player, selectedType, currentWeight);
            }
        }
    }

    private void openAnvilInputGui(Player player, RewardType type, int currentWeight) {
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    try {
                        String text = stateSnapshot.getText().trim();
                        int newWeight = Integer.parseInt(text);

                        if (newWeight < 0) {
                            return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("0 이상의 정수만!"));
                        }

                        // 가중치 저장 및 동기화 진행
                        configManager.setAndSaveWeight(type, newWeight);
                        stateSnapshot.getPlayer().sendMessage("§6[다이아 룰렛] §a성공적으로 §e" + type.getDisplayName() + "§a의 가중치가 §e" + newWeight + "§a(으)로 변경되었습니다.");

                        // [교정] 메인 스레드(싱크)에서 이전 세부 설정 GUI가 안정적으로 열리도록 버킷 스케줄러로 감싸줍니다.
                        return Arrays.asList(
                                AnvilGUI.ResponseAction.close(),
                                AnvilGUI.ResponseAction.run(() -> {
                                    Bukkit.getScheduler().runTask(PizRoullet.getInstance(), () -> {
                                        openSubSettingsGui(stateSnapshot.getPlayer());
                                    });
                                })
                        );

                    } catch (NumberFormatException e) {
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("숫자만 입력!"));
                    }
                })
                .text(String.valueOf(currentWeight))
                .title("§0" + type.getDisplayName() + " 가중치")
                .plugin(PizRoullet.getInstance())
                .open(player);
    }

    /**
     * 마지막 줄까지 깔끔하게 여백 처리를 끝낸 세부 설정창 인벤토리 빌더
     */
    private void openSubSettingsGui(Player player) {
        // 위아래 양옆 여백 및 빈 줄 프로토콜을 다 지키기 위해 대형 상자 크기(54칸)로 증설합니다.
        Inventory subGui = Bukkit.createInventory(null, 54, SUB_GUI_TITLE);
        RewardType[] types = RewardType.values();
        RouletteProbabilityCalculator pc = PizRoullet.getInstance().getProbabilityCalculator();

        // 1. 2번째 줄 정중앙(13번 슬롯)에 전체 확률 정보 버튼 배치
        Material ironBlock = Registry.MATERIAL.get(NamespacedKey.minecraft("iron_block"));
        if (ironBlock != null) {
            ItemStack infoItem = new ItemStack(ironBlock);
            ItemMeta infoMeta = infoItem.getItemMeta();
            if (infoMeta != null) {
                infoMeta.setDisplayName("§e§l[ 전체 룰렛 확률 현황 ]");

                List<String> lore = new ArrayList<>();
                lore.add("§7현재 활성화된 모든 벌칙의 실시간 확률입니다.");
                lore.add("§7총 가중치 합: §6" + pc.getTotalWeight());
                lore.add("");

                for (RewardType type : types) {
                    int weight = configManager.getWeight(type);
                    double pct = pc.getIndividualPercentage(type);

                    if (weight > 0) {
                        lore.add("§f• " + type.getDisplayName() + ": §a" + pct + "% §7(" + weight + ")");
                    } else {
                        lore.add("§f• " + type.getDisplayName() + ": §c비활성화 (Off)");
                    }
                }
                infoMeta.setLore(lore);
                infoItem.setItemMeta(infoMeta);
            }
            subGui.setItem(INFO_SLOT, infoItem);
        }

        // 2. 4번째 줄과 5번째 줄에 나누어 벌칙 배치 (3번째 줄 공백, 6번째 줄 공백 완벽 유지)
        for (int i = 0; i < types.length; i++) {
            if (i >= SLOTS.length) break;

            RewardType type = types[i];
            int currentWeight = configManager.getWeight(type);
            double currentPercentage = pc.getIndividualPercentage(type);
            String status = (currentWeight > 0) ? "§aOn" : "§cOff";

            Material material = getRewardMaterial(type);
            if (material == null) {
                material = Material.BARRIER;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b§l" + type.getDisplayName());
                meta.setLore(Arrays.asList(
                        "§7현재 상태: " + status,
                        "§7현재 가중치: §e" + currentWeight,
                        "§7현재 확률: §a" + currentPercentage + "%",
                        "",
                        "§e[클릭] §7모루 창에서 가중치 수정"
                ));
                item.setItemMeta(meta);
            }
            subGui.setItem(SLOTS[i], item);
        }

        player.openInventory(subGui);
    }

    private Material getRewardMaterial(RewardType type) {
        switch (type) {
            case DIAMOND_INCREASE:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("diamond"));
            case MONSTER_SPAWN:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("zombie_spawn_egg"));
            case LAVA_PLACE:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("lava_bucket"));
            case DROP_ALL:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("chest"));
            case RESIZE_PLAYER:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("potion"));
            case RANDOM_TELEPORT:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("ender_pearl"));
            case TNT_SPAWN:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("tnt"));
            case SAND_PRISON:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("sand"));
            case COBWEB_PRISON:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("cobweb"));
            case WATER_CLUTCH:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("water_bucket"));
            case JUMP_MAP:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("oak_stairs"));
            default:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("paper"));
        }
    }

    /**
     * 목표 다이아 개수를 입력받고 게임을 시작하는 모루 GUI 제어기
     */
    private void openTargetDiamondInputGui(Player player) {
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    try {
                        String text = stateSnapshot.getText().trim();
                        int target = Integer.parseInt(text);

                        if (target <= 0) {
                            return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("1 이상의 정수만!"));
                        }

                        // 메인 스레드에서 안전하게 게임 시작을 호출하고 창을 닫습니다.
                        return Arrays.asList(
                                AnvilGUI.ResponseAction.close(),
                                AnvilGUI.ResponseAction.run(() -> {
                                    Bukkit.getScheduler().runTask(PizRoullet.getInstance(), () -> {
                                        RouletteManager.startGame(target);
                                    });
                                })
                        );

                    } catch (NumberFormatException e) {
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("숫자만 입력!"));
                    }
                })
                .text("1000") // 입력창 초기 추천값
                .title("§0목표 다이아 개수 입력")
                .plugin(PizRoullet.getInstance())
                .open(player);
    }

}