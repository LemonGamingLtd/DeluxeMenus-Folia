package com.extendedclip.deluxemenus.dupe;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.listener.Listener;
import com.extendedclip.deluxemenus.utils.DebugLevel;
import io.github.projectunified.minelib.scheduler.entity.EntityScheduler;
import io.github.projectunified.minelib.scheduler.global.GlobalScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Prevents duplication of items created by DeluxeMenus. Items created by DeluxeMenus are marked and removed if found
 * outside the inventory they were created in.
 */
public class DupeFixer extends Listener {

    private final MenuItemMarker marker;

    public DupeFixer(@NotNull final DeluxeMenus plugin, @NotNull final MenuItemMarker marker) {
        super(plugin);
        this.marker = marker;
    }

    @EventHandler
    private void onInventoryClose(@NotNull final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getPlayer();
        EntityScheduler.get(DeluxeMenus.getInstance(), player).runLater(() -> {
            boolean removed = false;

            for (ItemStack itemStack : player.getInventory().getContents()) {
                if (itemStack == null || !marker.isMarked(itemStack)) {
                    continue;
                }

                player.getInventory().remove(itemStack);
                removed = true;

                plugin.debug(
                        DebugLevel.LOWEST,
                        Level.INFO,
                        "DeluxeMenus item found in main inventory on close. Removing it."
                );
            }

            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (marker.isMarked(offhand)) {
                player.getInventory().setItemInOffHand(null);
                removed = true;

                plugin.debug(
                        DebugLevel.LOWEST,
                        Level.INFO,
                        "DeluxeMenus item found in offhand on close. Removing it."
                );
            }

            if (removed) {
                player.updateInventory();
            }
        }, 1L);
    }

    @EventHandler
    private void onPickup(@NotNull final EntityPickupItemEvent event) {
        if (!marker.isMarked(event.getItem().getItemStack())) {
            return;
        }

        plugin.debug(
                DebugLevel.LOWEST,
                Level.INFO,
                "Someone picked up a DeluxeMenus item. Removing it."
        );
        event.getItem().remove();
    }

    @EventHandler
    private void onDrop(@NotNull final PlayerDropItemEvent event) {
        if (!marker.isMarked(event.getItemDrop().getItemStack())) {
            return;
        }

        plugin.debug(
                DebugLevel.LOWEST,
                Level.INFO,
                "A DeluxeMenus item was dropped in the world. Removing it."
        );
        event.getItemDrop().remove();
    }

    @EventHandler
    private void onLogin(@NotNull final PlayerLoginEvent event) {
        GlobalScheduler.get(plugin).runLater(
                () -> {
                    for (final ItemStack itemStack : event.getPlayer().getInventory().getContents()) {
                        if (itemStack == null) continue;
                        if (!marker.isMarked(itemStack)) continue;

                        plugin.debug(
                                DebugLevel.LOWEST,
                                Level.INFO,
                                "Player logged in with a DeluxeMenus item in their inventory. Removing it."
                        );
                        event.getPlayer().getInventory().remove(itemStack);
                    }},
                10L
        );
    }
}