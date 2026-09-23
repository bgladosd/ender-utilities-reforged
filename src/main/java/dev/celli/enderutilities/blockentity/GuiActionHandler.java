package dev.celli.enderutilities.blockentity;

import net.minecraft.world.entity.player.Player;

public interface GuiActionHandler {
    void performGuiAction(Player player, int action, int element);
}
