package com.pizroullet;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class RouletteCommand implements CommandExecutor {

    public static final String GUI_TITLE = "§0다이아룰렛 설정";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("게임 내부에서만 사용 가능합니다.");
            return true;
        }

        Player player = (Player) sender;
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        // [설정 변경] 버튼 - 철 블록 (11번 슬롯)
        Material ironBlock = Registry.MATERIAL.get(NamespacedKey.minecraft("iron_block"));
        if (ironBlock != null) {
            ItemStack settingsButton = new ItemStack(ironBlock);
            ItemMeta settingsMeta = settingsButton.getItemMeta();
            if (settingsMeta != null) {
                settingsMeta.setDisplayName("§b§l[ 세부 설정 변경 ]");
                settingsMeta.setLore(Arrays.asList(
                        "§7클릭하면 목표 다이아 개수 등",
                        "§7다양한 게임 설정값을 조정하는 창을 엽니다."
                ));
                settingsButton.setItemMeta(settingsMeta);
            }
            gui.setItem(11, settingsButton);
        }

        // [게임 시작] 버튼 - 라임색 양털 (13번 슬롯)
        Material limeWool = Registry.MATERIAL.get(NamespacedKey.minecraft("lime_wool"));
        if (limeWool != null) {
            ItemStack startButton = new ItemStack(limeWool);
            ItemMeta startMeta = startButton.getItemMeta();
            if (startMeta != null) {
                startMeta.setDisplayName("§a§l[ 게임 시작 ]");
                startMeta.setLore(Arrays.asList(
                        "§7클릭하면 다이아룰렛 게임을 시작합니다.",
                        "§7이후 들어오는 후원 메시지를 감지합니다."
                ));
                startButton.setItemMeta(startMeta);
            }
            gui.setItem(13, startButton);
        }

        // [게임 중단] 버튼 - 빨간색 양털 (15번 슬롯)
        Material redWool = Registry.MATERIAL.get(NamespacedKey.minecraft("red_wool"));
        if (redWool != null) {
            ItemStack stopButton = new ItemStack(redWool);
            ItemMeta stopMeta = stopButton.getItemMeta();
            if (stopMeta != null) {
                stopMeta.setDisplayName("§c§l[ 게임 중단 ]");
                stopMeta.setLore(Arrays.asList(
                        "§7진행 중인 게임을 강제로 종료합니다.",
                        "§7종료 시 후원 감지도 함께 비활성화됩니다."
                ));
                stopButton.setItemMeta(stopMeta);
            }
            gui.setItem(15, stopButton);
        }

        player.openInventory(gui);
        return true;
    }
}