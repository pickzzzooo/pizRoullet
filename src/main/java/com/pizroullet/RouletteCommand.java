package com.pizroullet;

import com.pizroullet.manager.RouletteConfigManager;
import com.pizroullet.manager.RouletteManager;
import com.pizroullet.util.RewardType;
import com.pizroullet.listner.ToonationListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouletteCommand implements CommandExecutor, TabCompleter {

    public static final String GUI_TITLE = "§0다이아룰렛 - 메인 설정";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("테스트")) {
                if (!sender.hasPermission("pizroullet.admin")) {
                    sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("§c[사용법] /다룰 테스트 [벌칙키] 또는 /다룰 테스트 큐추가 [숫자]");
                    return true;
                }

                String key = args[1];

                // [추가] 큐추가 명령어 처리
                if (key.equalsIgnoreCase("큐추가")) {
                    int count = 1; // 기본값 1번
                    if (args.length >= 3) {
                        try {
                            count = Integer.parseInt(args[2]);
                            if (count <= 0) {
                                sender.sendMessage("§c[오류] 숫자는 1 이상이어야 합니다.");
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§c[오류] 올바른 숫자를 입력해 주세요.");
                            return true;
                        }
                    }

                    if (!RouletteManager.isRunning()) {
                        sender.sendMessage("§c진행 중인 룰렛 게임이 없습니다. 먼저 게임을 시작하세요.");
                        return true;
                    }

                    // 지정된 횟수만큼 테스트용 큐 적재 (공용 API 활용)
                    for (int i = 0; i < count; i++) {
                        ToonationListener.addDonationToQueue("테스트유저");
                    }
                    sender.sendMessage("§a[테스트] 일반 룰렛 대기 큐에 §e" + count + "개§f의 데이터를 추가했습니다.");
                    return true;
                }

                // 기존 벌칙 단일 테스트 로직
                RewardType targetReward = RewardType.fromConfigKey(key);

                if (targetReward == null) {
                    sender.sendMessage("§c존재하지 않는 벌칙키입니다. 탭 완성을 확인하세요.");
                    return true;
                }

                ToonationListener.triggerTestRoulette(sender.getName(), targetReward);
                sender.sendMessage("§a[테스트] §f" + targetReward.getDisplayName() + " 벌칙 강제 테스트를 시작합니다.");
                return true;
            }
        }

        Player player = (Player) sender;
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);
        RouletteConfigManager config = PizRoullet.getInstance().getConfigManager();

        Material limeWool = Registry.MATERIAL.get(NamespacedKey.minecraft("lime_wool"));
        if (limeWool != null) {
            ItemStack item = new ItemStack(limeWool);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a§l[ 세부 설정 변경 ]");
                meta.setLore(Arrays.asList("§7룰렛에 포함될 벌칙들의", "§7확률 및 가중치를 설정합니다."));
                item.setItemMeta(meta);
            }
            gui.setItem(10, item);
        }

        Material emeraldBlock = Registry.MATERIAL.get(NamespacedKey.minecraft("emerald_block"));
        if (emeraldBlock != null) {
            ItemStack item = new ItemStack(emeraldBlock);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§2§l[ 게임 시작 ]");
                meta.setLore(Arrays.asList("§7목표 다이아 개수를 입력하고", "§7새로운 룰렛 게임을 시작합니다."));
                item.setItemMeta(meta);
            }
            gui.setItem(12, item);
        }

        Material goldBlock = Registry.MATERIAL.get(NamespacedKey.minecraft("gold_block"));
        if (goldBlock != null) {
            ItemStack goldItem = new ItemStack(goldBlock);
            ItemMeta goldMeta = goldItem.getItemMeta();
            if (goldMeta != null) {
                goldMeta.setDisplayName("§e§l[ 룰렛 발동 기준 금액 설정 ]");
                goldMeta.setLore(Arrays.asList(
                        "§7현재 설정된 금액: §a" + config.getMinDonationAmount() + "원",
                        "§7이 금액 이상의 후원이 들어오면 룰렛이 가동됩니다.",
                        "",
                        "§e[클릭] §7금액 수정하기"
                ));
                goldItem.setItemMeta(goldMeta);
            }
            gui.setItem(14, goldItem);
        }

        Material redstoneBlock = Registry.MATERIAL.get(NamespacedKey.minecraft("redstone_block"));
        if (redstoneBlock != null) {
            ItemStack item = new ItemStack(redstoneBlock);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§c§l[ 게임 중단 ]");
                meta.setLore(Arrays.asList("§7진행 중인 룰렛 게임을 강제로 종료하고", "§7스코어보드를 초기화합니다."));
                item.setItemMeta(meta);
            }
            gui.setItem(16, item);
        }

        player.openInventory(gui);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("시작");
            completions.add("설정");
            completions.add("테스트");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("테스트")) {
            completions.add("큐추가"); // 탭 완성 목록에 추가
            for (RewardType type : RewardType.values()) {
                completions.add(type.getConfigKey());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("테스트") && args[1].equalsIgnoreCase("큐추가")) {
            completions.add("1");
            completions.add("5");
            completions.add("10");
        }

        List<String> result = new ArrayList<>();
        String currentArg = args[args.length - 1].toLowerCase();
        for (String s : completions) {
            if (s.toLowerCase().startsWith(currentArg)) {
                result.add(s);
            }
        }
        return result;
    }
}