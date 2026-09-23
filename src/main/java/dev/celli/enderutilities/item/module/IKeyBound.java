package dev.celli.enderutilities.item.module;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IKeyBound {
    boolean doKeyBindingAction(Player player, ItemStack stack, int key);
}
