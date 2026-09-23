package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.block.HorizontalMachineBlock;
import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.menu.SoundBlockMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;

public class SoundBlockEntity extends InventoryBlockEntity implements GuiActionHandler,
        HorizontalMachineBlock.NeighborAwareBlockEntity {
    private float volume = 1.0F;
    private float pitch = 1.0F;
    private boolean repeat;
    private boolean redstoneState;
    private int selectedSound;
    private ResourceLocation selectedId = SoundEvents.NOTE_BLOCK_HARP.value().getLocation();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (volume * 100);
                case 1 -> (int) (pitch * 100);
                case 2 -> repeat ? 1 : 0;
                case 3 -> selectedSound;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> volume = value / 100.0F;
                case 1 -> pitch = value / 100.0F;
                case 2 -> repeat = value != 0;
                case 3 -> selectedSound = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public SoundBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOUND_BLOCK.get(), pos, state, new LimitedItemStackHandler(0, 64));
    }

    public ContainerData getData() {
        return data;
    }

    @Override
    public void dropContents() {
    }

    @Override
    public void onNeighborChanged() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered != redstoneState) {
            redstoneState = powered;
            if (powered) {
                playSound();
            }
            setChanged();
        }
    }

    public void playSound() {
        if (level == null) {
            return;
        }
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(selectedId);
        if (sound == null) {
            sound = soundAt(selectedSound);
        }
        if (sound == null) {
            return;
        }
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, Mth.clamp(volume, 0.0F, 4.0F), Mth.clamp(pitch, 0.0F, 20.0F));
    }

    private static SoundEvent soundAt(int index) {
        int size = BuiltInRegistries.SOUND_EVENT.size();
        if (size <= 0) {
            return SoundEvents.NOTE_BLOCK_HARP.value();
        }
        int i = 0;
        int target = Math.floorMod(index, size);
        for (SoundEvent event : BuiltInRegistries.SOUND_EVENT) {
            if (i++ == target) {
                return event;
            }
        }
        return SoundEvents.NOTE_BLOCK_HARP.value();
    }

    private void selectOffset(int delta) {
        int size = Math.max(1, BuiltInRegistries.SOUND_EVENT.size());
        selectedSound = Math.floorMod(selectedSound + delta, size);
        SoundEvent event = soundAt(selectedSound);
        selectedId = BuiltInRegistries.SOUND_EVENT.getKey(event);
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        switch (action) {
            case 0 -> volume = Mth.clamp(volume + element * 0.1F, 0.0F, 4.0F);
            case 1 -> pitch = Mth.clamp(pitch + element * 0.1F, 0.0F, 20.0F);
            case 2 -> repeat = !repeat;
            case 3 -> selectOffset(element);
            case 4 -> playSound();
            default -> {
            }
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Volume", volume);
        tag.putFloat("Pitch", pitch);
        tag.putBoolean("Repeat", repeat);
        tag.putBoolean("Powered", redstoneState);
        tag.putInt("Selected", selectedSound);
        if (selectedId != null) {
            tag.putString("Sound", selectedId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        volume = tag.getFloat("Volume");
        pitch = tag.getFloat("Pitch");
        repeat = tag.getBoolean("Repeat");
        redstoneState = tag.getBoolean("Powered");
        selectedSound = tag.getInt("Selected");
        if (tag.contains("Sound")) {
            selectedId = ResourceLocation.tryParse(tag.getString("Sound"));
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new SoundBlockMenu(id, playerInventory, this);
    }
}
