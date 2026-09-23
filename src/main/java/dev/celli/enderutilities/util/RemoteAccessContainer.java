package dev.celli.enderutilities.util;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Forwards a vanilla {@link Container} while skipping the 8-block {@code stillValid} check.
 * Bound tools (Ender Bag) chunk-load the target instead of requiring the player nearby.
 */
public final class RemoteAccessContainer implements Container {
    private final Container inner;

    public RemoteAccessContainer(Container inner) {
        this.inner = inner;
    }

    @Override
    public int getContainerSize() {
        return inner.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inner.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return inner.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inner.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inner.setItem(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return inner.getMaxStackSize();
    }

    @Override
    public void setChanged() {
        inner.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void startOpen(Player player) {
        inner.startOpen(player);
    }

    @Override
    public void stopOpen(Player player) {
        inner.stopOpen(player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return inner.canPlaceItem(slot, stack);
    }

    @Override
    public void clearContent() {
        inner.clearContent();
    }
}
