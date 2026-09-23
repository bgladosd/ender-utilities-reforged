package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.SoundBlockEntity;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class SoundBlockMenu extends EnderMenu {
    private final SoundBlockEntity soundBlock;
    private final ContainerData data;

    public SoundBlockMenu(int id, Inventory playerInventory, SoundBlockEntity soundBlock) {
        this(id, playerInventory, soundBlock, soundBlock.getData());
    }

    public SoundBlockMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, SoundBlockEntity.class), new SimpleContainerData(4));
    }

    private SoundBlockMenu(int id, Inventory playerInventory, SoundBlockEntity soundBlock, ContainerData data) {
        super(ModMenus.SOUND_BLOCK.get(), id);
        this.soundBlock = soundBlock;
        this.data = data;
        addPlayerSlots(playerInventory, 8, 174);
        addDataSlots(data);
    }

    public SoundBlockEntity getSoundBlock() {
        return soundBlock;
    }

    public ContainerData getData() {
        return data;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(soundBlock, player);
    }
}
