package com.pizroullet;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class RouletteGameListener implements Listener {

    private final NamespacedKey processedKey = new NamespacedKey(PizRoullet.getInstance(), "diamond_processed");

    /**
     * 1. 플레이어가 바닥에 떨어진 아이템을 주울 때 감지
     */
    @EventHandler
    public void onPlayerPickup(EntityPickupItemEvent event) {
        if (!RouletteManager.isRunning()) return;
        if (!(event.getEntity() instanceof Player)) return;

        Item itemEntity = event.getItem();
        ItemStack itemStack = itemEntity.getItemStack();

        if (itemStack.getType() == Material.DIAMOND) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta == null) return;

            // 이미 처리된 다이아몬드라면 무시
            if (meta.getPersistentDataContainer().has(processedKey, PersistentDataType.BYTE)) {
                return;
            }

            // 영수증 마킹 각인
            meta.getPersistentDataContainer().set(processedKey, PersistentDataType.BYTE, (byte) 1);
            itemStack.setItemMeta(meta);
            itemEntity.setItemStack(itemStack);

            // 주운 수량만큼 점수 추가
            RouletteManager.addDiamond(itemStack.getAmount());
        }
    }

    /**
     * 2. 플레이어가 아이템을 버릴 때 감지 (에러 수정 완료)
     */
    @EventHandler
    public void onPlayerDrop(EntityDropItemEvent event) {
        if (!RouletteManager.isRunning()) return;
        if (!(event.getEntity() instanceof Player)) return;

        Item itemEntity = event.getItemDrop();
        if (itemEntity == null) return;

        ItemStack itemStack = itemEntity.getItemStack();

        if (itemStack.getType() == Material.DIAMOND) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                // 바닥에 떨어지는 순간 마킹을 무조건 주입하여 꼼수 원천 차단
                meta.getPersistentDataContainer().set(processedKey, PersistentDataType.BYTE, (byte) 1);
                itemStack.setItemMeta(meta);
                itemEntity.setItemStack(itemStack);
            }
        }
    }

    /**
     * 3. 상자 등 외부 인벤토리에서 내 인벤토리로 다이아몬드를 클릭해서 가져올 때 감지
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!RouletteManager.isRunning()) return;

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null || currentItem.getType() != Material.DIAMOND) return;

        // 인벤토리 내부 클릭 메커니즘 꼼수 방지 (단순 자리 이동이나 내 인벤토리 내부 클릭은 무시)
        InventoryAction action = event.getAction();
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                action == InventoryAction.PICKUP_ALL ||
                action == InventoryAction.PICKUP_HALF ||
                action == InventoryAction.PICKUP_ONE) {

            // 상자나 외부 보관함 칸을 클릭했는지 확인
            if (event.getClickedInventory() != event.getView().getBottomInventory()) {
                ItemMeta meta = currentItem.getItemMeta();
                if (meta != null) {
                    // 마킹이 없는 순수 다이아몬드를 상자에서 꺼낼 때만 카운트
                    if (!meta.getPersistentDataContainer().has(processedKey, PersistentDataType.BYTE)) {
                        meta.getPersistentDataContainer().set(processedKey, PersistentDataType.BYTE, (byte) 1);
                        currentItem.setItemMeta(meta);

                        RouletteManager.addDiamond(currentItem.getAmount());
                    }
                }
            }
        }
    }
}