package dev.celli.enderutilities.blockentity;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.item.ItemLinkCrystal;
import dev.celli.enderutilities.menu.PortalPanelMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import dev.celli.enderutilities.registry.ModBlocks;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.PortalFormer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class PortalPanelBlockEntity extends InventoryBlockEntity implements GuiActionHandler {
    private int activeTargetId;
    private int portalTargetId;
    private final int[] colors = new int[9];

    public PortalPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORTAL_PANEL.get(), pos, state, new LimitedItemStackHandler(16, 1));
        java.util.Arrays.fill(colors, 0xFFFFFF);
        inventory.validator(this::isItemValid).onChanged(() -> {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        });
    }

    public int getActiveTargetId() {
        return activeTargetId;
    }

    public void setActiveTargetId(int target) {
        this.activeTargetId = Mth.clamp(target, 0, 7);
    }

    public int getColor(int target) {
        if (level != null && !level.isClientSide) {
            return getPortalColorForTargetId(target);
        }
        return colors[Mth.clamp(target, 0, 8)];
    }

    public String getPanelDisplayName() {
        return getTargetName(getActiveTargetId());
    }

    public void setTargetName(String name) {
        int id = getActiveTargetId();
        if (id < 0 || id >= 8) {
            return;
        }
        ItemStack stack = inventory.getStackInSlot(id);
        if (stack.isEmpty()) {
            return;
        }
        if (StringUtils.isBlank(name)) {
            stack.remove(DataComponents.CUSTOM_NAME);
        } else {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
        inventory.setStackInSlot(id, stack);
    }

    public boolean targetIsPortal() {
        ItemStack stack = inventory.getStackInSlot(getActiveTargetId());
        return !stack.isEmpty()
                && stack.getItem() instanceof ItemLinkCrystal module
                && module.getModuleTier(stack) == ItemLinkCrystal.TYPE_PORTAL;
    }

    private TargetLocation getActiveTarget() {
        ItemStack stack = inventory.getStackInSlot(getActiveTargetId());
        return stack.isEmpty() ? null : stack.get(ModDataComponents.TARGET);
    }

    private OwnerInfo getOwner() {
        ItemStack stack = inventory.getStackInSlot(getActiveTargetId());
        return stack.isEmpty() ? null : stack.get(ModDataComponents.OWNER);
    }

    private int getPortalColor() {
        return getPortalColorForTargetId(getActiveTargetId());
    }

    private int getPortalColorForTargetId(int targetId) {
        DyeColor dye = getDyeForTargetId(targetId);
        return dye != null ? dye.getMapColor().col : 0xFFFFFFFF;
    }

    @Nullable
    private DyeColor getDyeForTargetId(int targetId) {
        if (targetId == 8) {
            targetId = getActiveTargetId();
        }
        if (targetId < 0 || targetId >= 8) {
            return null;
        }
        ItemStack stack = inventory.getStackInSlot(targetId + 8);
        if (stack.isEmpty() || !(stack.getItem() instanceof DyeItem dyeItem)) {
            return null;
        }
        return dyeItem.getDyeColor();
    }

    public String getTargetDisplayName(int targetId) {
        return getTargetName(targetId);
    }

    private String getTargetName(int targetId) {
        if (targetId < 0 || targetId > 7) {
            return "";
        }
        ItemStack stack = inventory.getStackInSlot(targetId);
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    private boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot < 8) {
            if (!(stack.getItem() instanceof ItemLinkCrystal module)) {
                return false;
            }
            int tier = module.getModuleTier(stack);
            return tier == ItemLinkCrystal.TYPE_PORTAL
                    || (tier == ItemLinkCrystal.TYPE_LOCATION && !ModCommonConfig.PORTAL_ONLY_ALLOWS_PORTAL_TYPE_LINK_CRYSTALS.get());
        }
        return stack.getItem() instanceof DyeItem;
    }

    public void tryTogglePortal() {
        if (level == null || level.isClientSide) {
            return;
        }
        Level world = level;
        BlockPos posPanel = worldPosition;
        BlockPos posFrame = posPanel.relative(getBlockState().getValue(BlockStateProperties.FACING).getOpposite());
        PortalFormer portalFormer = new PortalFormer(world, posFrame, ModBlocks.PORTAL_FRAME.get(), ModBlocks.PORTAL.get());
        portalFormer.setPortalData(new PortalBlockEntity.PortalData(
                getActiveTarget(), getOwner(), getPortalColor(), targetIsPortal()));
        portalFormer.analyzePortal();
        boolean state = portalFormer.getPortalState();
        boolean recreate = this.activeTargetId != this.portalTargetId;
        if (!state) {
            if (portalFormer.togglePortalState(false)) {
                this.portalTargetId = this.activeTargetId;
                world.playSound(null, posPanel, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.5f, 1.0f);
            }
        } else if (portalFormer.togglePortalState(recreate)) {
            if (recreate) {
                this.portalTargetId = this.activeTargetId;
                world.playSound(null, posPanel, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.5f, 1.0f);
            } else {
                world.playSound(null, posPanel, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.4f, 0.85f);
            }
        }
        setChanged();
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        if (action == 0 && element >= 0 && element < 8) {
            setActiveTargetId(element);
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("SelectedTarget", (byte) activeTargetId);
        tag.putByte("PortalTarget", (byte) portalTargetId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setActiveTargetId(tag.getByte("SelectedTarget"));
        portalTargetId = tag.getByte("PortalTarget");
        for (int i = 0; i < 9; i++) {
            colors[i] = getPortalColorForTargetId(i);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.saveWithoutMetadata(registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PortalPanelMenu(id, inventory, this);
    }
}
