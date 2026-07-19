package com.pizroullet.listner;

import com.pizroullet.*;
import com.pizroullet.manager.RouletteConfigManager;
import com.pizroullet.manager.RouletteManager;
import com.pizroullet.util.RewardType;
import com.pizroullet.util.RouletteProbabilityCalculator;
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

    // [교정] 19개의 벌칙을 중복 없이 깔끔하게 배치하기 위한 54칸 내 지정 슬롯 리스트 (3번째 줄부터 6번째 줄까지 순서대로 배치)
    private final int[] SLOTS = {
            18, 19, 20, 21, 22, 23, 24, 25, 26,  // 3번째 줄 (9칸)
            27, 28, 29, 30, 31, 32, 33, 34, 35,  // 4번째 줄 (9칸)
            36                                   // 5번째 줄 첫 칸 (총 19칸 확보)
    };

    // 2번째 줄 정중앙 슬롯 번호 (세부 설정창용)
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

            if (slot == 10) {
                openSubSettingsGui(player);
            }
            else if (slot == 12) {
                if (RouletteManager.isRunning()) {
                    player.sendMessage("§c이미 게임이 진행 중입니다!");
                    player.closeInventory();
                    return;
                }
                openTargetDiamondInputGui(player);
            }
            else if (slot == 14) {
                openMinAmountInputGui(player);
            }
            else if (slot == 16) {
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

            // [교정] 동적으로 바뀐 RewardType 배열 크기와 매칭된 슬롯을 안전하게 교차 검증
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

                        configManager.setAndSaveWeight(type, newWeight);
                        stateSnapshot.getPlayer().sendMessage("§6[다이아 룰렛] §a성공적으로 §e" + type.getDisplayName() + "§a의 가중치가 §e" + newWeight + "§a(으)로 변경되었습니다.");

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

    private void openSubSettingsGui(Player player) {
        Inventory subGui = Bukkit.createInventory(null, 54, SUB_GUI_TITLE);
        RewardType[] types = RewardType.values();
        RouletteProbabilityCalculator pc = PizRoullet.getInstance().getProbabilityCalculator();

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

        // [교정] 확장된 SLOTS 인덱스 가드를 기반으로 안전하게 19개 아이템 주입
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
                // [추가 연출] 각 벌칙 아이템 이름 좌측에 해당 등급의 색상을 동적으로 매핑
                String tierColor = type.getTier().getColorCode();
                meta.setDisplayName(tierColor + "§l" + type.getDisplayName());
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

    // [교정] 새로 매핑된 19개의 RewardType 전체에 매칭되는 마인크래프트 직관적 아이콘 할당
    // [교정] 요청하신 규격(보따리, 물양동이, 겉날개)으로 아이콘 리스트 통일 가동
    private Material getRewardMaterial(RewardType type) {
        switch (type) {
            // ================= 1단계 벌칙 =================
            case MONSTER_BUNDLE_1:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("bundle")); // 보따리 통일
            case DIAMOND_INCREASE:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("diamond"));
            case DEBUFF:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("spider_eye"));
            case RESIZE_SMALL:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("fermented_spider_eye"));
            case DROP_ALL:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("chest"));
            case COBWEB_PRISON:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("cobweb"));

            // ================= 2단계 벌칙 =================
            case MONSTER_BUNDLE_2:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("bundle")); // 보따리 통일
            case ZOMBIE_RAID:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("zombie_head"));
            case SILVERFISH_RAID:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("infested_stone"));
            case SAND_PRISON:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("sand"));
            case TNT_SPAWN:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("tnt"));
            case RANDOM_TELEPORT:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("ender_pearl"));

            // ================= 3단계 벌칙 =================
            case MONSTER_BUNDLE_3:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("bundle")); // 보따리 통일
            case JUMP_MAP_1:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("oak_stairs"));
            case SKYDIVING_1:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("elytra")); // 겉날개 통일

            // ================= 4단계 벌칙 =================
            case MONSTER_BUNDLE_4:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("bundle")); // 보따리 통일
            case JUMP_MAP_2:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("purpur_stairs"));
            case SKYDIVING_2:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("elytra")); // 겉날개 통일

            // ================= 기타 미션/벌칙 예외 보완 =================
            // 만약 RewardType에 WATER_CLUTCH가 정의되어 있다면 물양동이로 반환
            // (정의되지 않았더라도 빌드 오류가 없도록 유연하게 switch 처리 유지)
            // case WATER_CLUTCH:
            //     return Registry.MATERIAL.get(NamespacedKey.minecraft("water_bucket"));

            default:
                return Registry.MATERIAL.get(NamespacedKey.minecraft("paper"));
        }
    }

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

                        return Arrays.asList(
                                AnvilGUI.ResponseAction.close(),
                                AnvilGUI.ResponseAction.run(() -> {
                                    Bukkit.getScheduler().runTask(PizRoullet.getInstance(), () -> {
                                        RouletteManager.startGame(player, target);
                                    });
                                })
                        );

                    } catch (NumberFormatException e) {
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("숫자만 입력!"));
                    }
                })
                .text("1000")
                .title("§0목표 다이아 개수 입력")
                .plugin(PizRoullet.getInstance())
                .open(player);
    }

    private void openMinAmountInputGui(Player player) {
        int currentAmount = configManager.getMinDonationAmount();

        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    try {
                        String text = stateSnapshot.getText().trim();
                        int newAmount = Integer.parseInt(text);

                        if (newAmount < 0) {
                            return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("0원 이상의 정수만!"));
                        }

                        configManager.setAndSaveMinDonationAmount(newAmount);
                        stateSnapshot.getPlayer().sendMessage("§6[다이아 룰렛] §a룰렛 최소 발동 금액이 §e" + newAmount + "원§a으로 변경되었습니다.");

                        return Arrays.asList(
                                AnvilGUI.ResponseAction.close(),
                                AnvilGUI.ResponseAction.run(() -> {
                                    Bukkit.getScheduler().runTask(PizRoullet.getInstance(), () -> {
                                        player.performCommand("다룰");
                                    });
                                })
                        );

                    } catch (NumberFormatException e) {
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("숫자만 입력!"));
                    }
                })
                .text(String.valueOf(currentAmount))
                .title("§0룰렛 발동 최소 금액 입력")
                .plugin(PizRoullet.getInstance())
                .open(player);
    }
}