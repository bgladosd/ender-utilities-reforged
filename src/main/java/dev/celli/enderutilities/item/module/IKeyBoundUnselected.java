package dev.celli.enderutilities.item.module;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IKeyBoundUnselected {
    boolean doUnselectedKeyAction(Player player, ItemStack stack, int key);
}
