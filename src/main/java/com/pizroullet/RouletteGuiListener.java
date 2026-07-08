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

import java.util.Arrays;
import java.util.Collections;

public class RouletteGuiListener implements Listener {

    public static final String SUB_GUI_TITLE = "§0다이아룰렛 - 세부 설정";
    private final RouletteConfigManager configManager = PizRoullet.getInstance().getConfigManager();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // 관리 대상 GUI 타이틀이 아니라면 즉시 리턴하여 무시
        if (!title.equals(RouletteCommand.GUI_TITLE) && !title.equals(SUB_GUI_TITLE)) {
            return;
        }

        // 클릭한 슬롯이 비어있다면 무시
        if (event.getCurrentItem() == null) {
            return;
        }

        // 아이템을 꺼내 가거나 인벤토리가 흐트러지지 않도록 이벤트 취소
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        // 1. 메인 설정 GUI (27칸짜리 화면) 처리
        if (title.equals(RouletteCommand.GUI_TITLE)) {
            int slot = event.getSlot();

            // 11번 슬롯(철 블록)을 클릭했을 때만 세부 설정 인벤토리를 오픈합니다.
            if (slot == 11) {
                openSubSettingsGui(player);
            }
            // 13번(시작), 15번(중단) 등의 다른 버튼 클릭 처리는 필요시 이곳에 구현됩니다.
        }

        // 2. 세부 설정 GUI (18칸짜리 화면) 처리
        else if (title.equals(SUB_GUI_TITLE)) {
            int slot = event.getSlot();
            RewardType[] types = RewardType.values();

            // 클릭한 슬롯 번호가 벌칙 Enum 배열의 범위 내에 있는지 안전하게 체크합니다.
            if (slot >= 0 && slot < types.length) {
                RewardType selectedType = types[slot];
                int currentWeight = configManager.getWeight(selectedType);

                // 오직 클릭한 '그 벌칙' 데이터만 들고 모루 입력창을 생성하여 열어줍니다.
                openAnvilInputGui(player, selectedType, currentWeight);
            }
        }
    }

    /**
     * 선택한 단 하나의 벌칙 가중치만 입력받아 변경하는 모루 GUI 제어기
     */
    private void openAnvilInputGui(Player player, RewardType type, int currentWeight) {
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    // 모루창의 가장 오른쪽 칸(결과물 출력부)을 마우스로 눌렀을 때만 작동
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    try {
                        String text = stateSnapshot.getText().trim();
                        int newWeight = Integer.parseInt(text);

                        // 방어 코드: 음수 입력 제한
                        if (newWeight < 0) {
                            return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("0 이상의 정수만!"));
                        }

                        // [핵심] 딱 선택한 해당 벌칙 객체(type)의 값만 파일 및 메모리에 동기화 갱신
                        configManager.setAndSaveWeight(type, newWeight);
                        stateSnapshot.getPlayer().sendMessage("§6[다이아 룰렛] §a성공적으로 §e" + type.getDisplayName() + "§a의 가중치가 §e" + newWeight + "§a(으)로 변경되었습니다.");

                        // 입력 성공 시 모루 창을 닫고, 세부 설정 창을 리프레시하여 다시 보여줍니다.
                        return Arrays.asList(
                                AnvilGUI.ResponseAction.close(),
                                AnvilGUI.ResponseAction.run(() -> openSubSettingsGui(stateSnapshot.getPlayer()))
                        );

                    } catch (NumberFormatException e) {
                        // 알파벳이나 한글 등 정수 파싱 에러 발생 시 입력 필드 텍스트 스왑
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("숫자만 입력!"));
                    }
                })
                .text(String.valueOf(currentWeight)) // 열렸을 때 최초로 보일 텍스트 창 기본값
                .title("§0" + type.getDisplayName() + " 가중치") // 어떤 벌칙을 수정 중인지 모루창 상단에 표기
                .plugin(PizRoullet.getInstance())
                .open(player);
    }

    /**
     * 벌칙 아이템들이 배열 순서대로 나열되는 세부 설정창 인벤토리 빌더
     */
    private void openSubSettingsGui(Player player) {
        Inventory subGui = Bukkit.createInventory(null, 18, SUB_GUI_TITLE);
        RewardType[] types = RewardType.values();
        Material paper = Registry.MATERIAL.get(NamespacedKey.minecraft("paper"));

        if (paper != null) {
            for (int i = 0; i < types.length; i++) {
                RewardType type = types[i];
                int currentWeight = configManager.getWeight(type);
                String status = (currentWeight > 0) ? "§aOn" : "§cOff";

                ItemStack item = new ItemStack(paper);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§b§l" + type.getDisplayName());
                    meta.setLore(Arrays.asList(
                            "§7현재 상태: " + status,
                            "§7현재 가중치: §e" + currentWeight,
                            "",
                            "§e[클릭] §7모루 창에서 가중치 수정"
                    ));
                    item.setItemMeta(meta);
                }
                // 각 벌칙 객체들이 0번 슬롯부터 순차적으로 정렬되어 배치됩니다.
                subGui.setItem(i, item);
            }
        }
        player.openInventory(subGui);
    }
}