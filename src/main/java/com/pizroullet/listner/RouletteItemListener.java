package com.pizroullet.listner;

import com.pizroullet.PizRoullet;
import com.pizroullet.manager.RouletteManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class RouletteItemListener implements Listener {

    /**
     * 플레이어 사망 시 룰렛 곡괭이가 바닥에 떨어지는 것을 방지하고 증발시킵니다.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!RouletteManager.isRunning()) return;

        Player player = event.getEntity();
        // 현재 게임 진행 중인 참여자가 죽은 경우에만 작동
        if (!player.getUniqueId().equals(RouletteManager.getParticipant().getUniqueId())) return;

        NamespacedKey key = new NamespacedKey(PizRoullet.getInstance(), "roulette_pickaxe");

        // 사망 시 떨어지는 드롭 아이템 목록에서 특수 곡괭이 제거
        event.getDrops().removeIf(item ->
                item != null &&
                        item.hasItemMeta() &&
                        item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)
        );
    }

    /**
     * 참여자가 리스폰할 때 특수 곡괭이를 새로 생성하여 다시 지급합니다.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!RouletteManager.isRunning()) return;

        Player player = event.getPlayer();
        if (RouletteManager.getParticipant() == null) return;
        if (!player.getUniqueId().equals(RouletteManager.getParticipant().getUniqueId())) return;

        // 리스폰 즉시 지급 시 타이밍 이슈 방지를 위해 1틱 뒤 메인 스레드에서 안전하게 인벤토리에 주입
        org.bukkit.Bukkit.getScheduler().runTaskLater(PizRoullet.getInstance(), () -> {
            if (player.isOnline()) {
                ItemStack specialPickaxe = new ItemStack(org.bukkit.Material.DIAMOND_PICKAXE);
                ItemMeta meta = specialPickaxe.getItemMeta();

                if (meta != null) {
                    meta.setUnbreakable(true);

                    NamespacedKey key = new NamespacedKey(PizRoullet.getInstance(), "roulette_pickaxe");
                    meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

                    specialPickaxe.setItemMeta(meta);
                }

                player.getInventory().addItem(specialPickaxe);
            }
        }, 1L);
    }

    /**
     * 참여자가 룰렛 전용 특수 곡괭이를 인벤토리 밖으로 버리는 것을 방지합니다.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!RouletteManager.isRunning()) return;

        Player player = event.getPlayer();
        if (RouletteManager.getParticipant() == null) return;
        if (!player.getUniqueId().equals(RouletteManager.getParticipant().getUniqueId())) return;

        ItemStack item = event.getItemDrop().getItemStack();
        NamespacedKey key = new NamespacedKey(PizRoullet.getInstance(), "roulette_pickaxe");

        // 버리려는 아이템이 특수 곡괭이인 경우 이벤트 취소
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }
}