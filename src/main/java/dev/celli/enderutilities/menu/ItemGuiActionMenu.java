package dev.celli.enderutilities.menu;

import net.minecraft.world.entity.player.Player;

public interface ItemGuiActionMenu {
    void performItemGuiAction(Player player, int action, int element);
}
