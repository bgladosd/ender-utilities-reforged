package dev.celli.enderutilities.menu;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.items.IItemHandler;

public abstract class EnderMenu extends AbstractContainerMenu {
    protected EnderMenu(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    protected void addPlayerSlots(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, x + col * 18, y + 58));
        }
    }

    protected void addOffhandSlot(Inventory inventory, int x, int y) {
        this.addSlot(new Slot(inventory, Inventory.SLOT_OFFHAND, x, y));
    }

    protected ItemStack quickMoveMachine(Player player, int index, int machineSlots) {
        ItemStack extracted = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            extracted = current.copy();
            if (index < machineSlots) {
                if (!this.moveItemStackTo(current, machineSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, machineSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (current.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return extracted;
    }

    public static boolean stillValid(BlockEntity be, Player player) {
        return be.getLevel() != null
                && be.getLevel().getBlockEntity(be.getBlockPos()) == be
                && player.distanceToSqr(be.getBlockPos().getCenter()) <= 64.0;
    }

    public static <T extends BlockEntity> T getBlockEntity(Inventory inventory, @Nullable FriendlyByteBuf buf, Class<T> type) {
        return getBlockEntity(inventory, readMenuBlockPos(inventory, buf), type);
    }

    public static <T extends BlockEntity> T getBlockEntity(Inventory inventory, BlockPos pos, Class<T> type) {
        if (type.isInstance(inventory.player.level().getBlockEntity(pos))) {
            return type.cast(inventory.player.level().getBlockEntity(pos));
        }
        throw new IllegalStateException(type.getSimpleName() + " missing at " + pos);
    }

    /**
     * Extra menu bytes are only present when the server sent {@code AdvancedOpenScreenPayload}.
     * Vanilla {@code open_screen} (and REI wrapping it) calls the factory with a null buffer.
     */
    public static BlockPos readMenuBlockPos(Inventory inventory, @Nullable FriendlyByteBuf buf) {
        if (buf != null) {
            return buf.readBlockPos();
        }
        return lookedAtBlockPos(inventory.player);
    }

    public static String readMenuUtf(@Nullable FriendlyByteBuf buf) {
        return buf != null ? buf.readUtf() : "";
    }

    public static InteractionHand readMenuHand(@Nullable FriendlyByteBuf buf) {
        return buf != null ? buf.readEnum(InteractionHand.class) : InteractionHand.MAIN_HAND;
    }

    public static int readMenuVarInt(@Nullable FriendlyByteBuf buf, int fallback) {
        return buf != null ? buf.readVarInt() : fallback;
    }

    public static BlockPos lookedAtBlockPos(Player player) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() != HitResult.Type.MISS) {
            return blockHit.getBlockPos();
        }
        return player.blockPosition();
    }

    public static void dropContents(IItemHandler inventory, net.minecraft.world.level.Level level, BlockPos pos) {
        if (level == null) {
            return;
        }
        for (int i = 0; i < inventory.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
        }
    }
}
