package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.block.HorizontalMachineBlock;
import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.menu.EnderFurnaceMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class EnderFurnaceBlockEntity extends InventoryBlockEntity implements GuiActionHandler,
        HorizontalMachineBlock.ServerTickingBlockEntity {
    public static final int COOKTIME_INC_NOFUEL = 1;
    public static final int COOKTIME_INC_SLOW = 20;
    public static final int COOKTIME_INC_FAST = 60;
    public static final int COOKTIME_DEFAULT = 1200;
    public static final int BURNTIME_USAGE_SLOW = 20;
    public static final int BURNTIME_USAGE_FAST = 120;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;

    private boolean fastMode;
    private boolean outputToEnderChest;
    private int burnTimeRemaining;
    private int burnTimeFresh;
    private int cookTime;
    private OwnerInfo owner;
    private int outputTimer;
    private ItemStack smeltingResult = ItemStack.EMPTY;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> burnTimeRemaining;
                case 2 -> burnTimeFresh;
                case 3 -> (fastMode ? 1 : 0) | (outputToEnderChest ? 2 : 0);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cookTime = value;
                case 1 -> burnTimeRemaining = value;
                case 2 -> burnTimeFresh = value;
                case 3 -> {
                    fastMode = (value & 1) != 0;
                    outputToEnderChest = (value & 2) != 0;
                }
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public EnderFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENDER_FURNACE.get(), pos, state, new LimitedItemStackHandler(3, 1024));
        inventory.onChanged(this::setChanged);
    }

    public ContainerData getData() {
        return data;
    }

    public boolean isFastMode() {
        return fastMode;
    }

    public String getOwnerName() {
        return owner == null ? "" : owner.name();
    }

    public void setOwner(Player player) {
        if (owner == null && player != null) {
            owner = OwnerInfo.of(player, true);
            setChanged();
        }
    }

    @Override
    public IItemHandler getHopperHandler() {
        return getHopperHandler(null);
    }

    public IItemHandler getHopperHandler(@Nullable Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 3;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return inventory.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (side == Direction.UP) {
                    if (slot != SLOT_INPUT || getResult(stack).isEmpty()) {
                        return stack;
                    }
                    return inventory.insertItem(SLOT_INPUT, stack, simulate);
                }
                if (slot != SLOT_FUEL || !isItemFuel(stack)) {
                    return stack;
                }
                return inventory.insertItem(SLOT_FUEL, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (side == Direction.UP) {
                    return ItemStack.EMPTY;
                }
                if (slot == SLOT_OUTPUT && !outputToEnderChest) {
                    return inventory.extractItem(SLOT_OUTPUT, amount, simulate);
                }
                if (slot == SLOT_FUEL && !isItemFuel(inventory.getStackInSlot(SLOT_FUEL))) {
                    return inventory.extractItem(SLOT_FUEL, amount, simulate);
                }
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return inventory.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == SLOT_INPUT ? !getResult(stack).isEmpty() : slot == SLOT_FUEL && isItemFuel(stack);
            }
        };
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        smeltingResult = getResult(input);
        boolean canSmelt = canSmelt();
        boolean hasFuel = consumeFuel(true) > 0;
        int increment = COOKTIME_INC_SLOW;
        if (burnTimeRemaining == 0 && !hasFuel) {
            increment = COOKTIME_INC_NOFUEL;
        } else if (fastMode) {
            increment = COOKTIME_INC_FAST;
        }
        boolean dirty = false;
        if (burnTimeRemaining > 0) {
            int use = fastMode ? BURNTIME_USAGE_FAST : BURNTIME_USAGE_SLOW;
            if (use > burnTimeRemaining) {
                if (hasFuel && canSmelt) {
                    burnTimeRemaining += consumeFuel(false);
                    hasFuel = consumeFuel(true) > 0;
                } else {
                    increment = (burnTimeRemaining * increment) / use;
                    use = burnTimeRemaining;
                }
            }
            burnTimeRemaining -= use;
            dirty = true;
        } else if (canSmelt && hasFuel) {
            burnTimeRemaining += consumeFuel(false);
            dirty = true;
        }
        if (canSmelt) {
            cookTime += increment;
            if (cookTime >= COOKTIME_DEFAULT) {
                smeltItem();
                canSmelt = canSmelt();
                cookTime = canSmelt && cookTime > COOKTIME_DEFAULT ? cookTime - COOKTIME_DEFAULT : 0;
            }
            if (burnTimeRemaining == 0 && consumeFuel(true) > 0 && canSmelt) {
                burnTimeRemaining += consumeFuel(false);
            }
            dirty = true;
        } else if (cookTime > 0) {
            cookTime -= Math.min(cookTime, COOKTIME_INC_SLOW / 2);
            dirty = true;
        }
        if (outputToEnderChest && !inventory.getStackInSlot(SLOT_OUTPUT).isEmpty() && ++outputTimer >= 20) {
            outputTimer = 0;
            dirty |= moveToEnderChest();
        }
        if (dirty) {
            setChanged();
        }
    }

    private boolean canSmelt() {
        if (smeltingResult.isEmpty() || inventory.getStackInSlot(SLOT_INPUT).isEmpty()) {
            return false;
        }
        return inventory.insertItem(SLOT_OUTPUT, smeltingResult.copy(), true).isEmpty();
    }

    private void smeltItem() {
        if (!canSmelt()) {
            return;
        }
        inventory.insertItem(SLOT_OUTPUT, smeltingResult.copy(), false);
        inventory.extractItem(SLOT_INPUT, 1, false);
    }

    private ItemStack getResult(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
                .map(RecipeHolder::value)
                .map(recipe -> recipe.getResultItem(level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private int consumeFuel(boolean simulate) {
        ItemStack stack = inventory.getStackInSlot(SLOT_FUEL);
        if (stack.isEmpty()) {
            return 0;
        }
        int lava = consumeLava(stack, simulate);
        if (lava > 0) {
            if (!simulate) {
                burnTimeFresh = lava;
            }
            return lava;
        }
        int burn = getItemBurnTime(stack);
        if (burn <= 0) {
            return 0;
        }
        if (!simulate) {
            ItemStack extracted = inventory.extractItem(SLOT_FUEL, 1, false);
            ItemStack remainder = extracted.getCraftingRemainingItem();
            if (!remainder.isEmpty()) {
                inventory.insertItem(SLOT_FUEL, remainder, false);
            }
            burnTimeFresh = burn;
        }
        return burn;
    }

    private int consumeLava(ItemStack stack, boolean simulate) {
        if (stack.getCount() > 1) {
            return 0;
        }
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return 0;
        }
        var drained = handler.drain(net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty() || drained.getFluid() != Fluids.LAVA) {
            return 0;
        }
        if (!simulate) {
            ItemStack copy = stack.copy();
            IFluidHandlerItem live = copy.getCapability(Capabilities.FluidHandler.ITEM);
            if (live != null) {
                var taken = live.drain(net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
                inventory.setStackInSlot(SLOT_FUEL, live.getContainer());
                return taken.getAmount() * 15 * COOKTIME_DEFAULT / 100;
            }
        }
        return drained.getAmount() * 15 * COOKTIME_DEFAULT / 100;
    }

    public static boolean isItemFuel(ItemStack stack) {
        return getItemBurnTime(stack) > 0 || hasLava(stack);
    }

    private static boolean hasLava(ItemStack stack) {
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return false;
        }
        var drained = handler.drain(1, IFluidHandler.FluidAction.SIMULATE);
        return !drained.isEmpty() && drained.getFluid() == Fluids.LAVA;
    }

    public static int getItemBurnTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(Items.ENDER_PEARL)) {
            return COOKTIME_DEFAULT * 8;
        }
        if (stack.is(Items.ENDER_EYE)) {
            return COOKTIME_DEFAULT * 17;
        }
        if (stack.is(Items.BLAZE_POWDER)) {
            return COOKTIME_DEFAULT * 9;
        }
        int vanilla = stack.getBurnTime(RecipeType.SMELTING);
        if (vanilla > 0) {
            return vanilla * COOKTIME_DEFAULT * 3 / 400;
        }
        return 0;
    }

    private boolean moveToEnderChest() {
        if (level == null || owner == null) {
            return false;
        }
        Player player = level.getPlayerByUUID(owner.owner());
        if (player == null) {
            return false;
        }
        ItemStack stack = inventory.extractItem(SLOT_OUTPUT, 64, false);
        if (stack.isEmpty()) {
            return false;
        }
        int orig = stack.getCount();
        ItemStack leftover = ItemHandlerHelper.insertItemStacked(new InvWrapper(player.getEnderChestInventory()), stack, false);
        boolean moved = leftover.getCount() != orig || leftover.isEmpty();
        if (!leftover.isEmpty()) {
            inventory.insertItem(SLOT_OUTPUT, leftover, false);
        }
        return moved;
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        if (action == 0) {
            fastMode = !fastMode;
        } else if (action == 1) {
            outputToEnderChest = !outputToEnderChest;
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        byte flags = 0;
        if (fastMode) {
            flags |= 0x01;
        }
        if (outputToEnderChest) {
            flags |= 0x02;
        }
        tag.putByte("Flags", flags);
        tag.putShort("BurnTimeRemaining", (short) burnTimeRemaining);
        tag.putShort("BurnTimeFresh", (short) burnTimeFresh);
        tag.putShort("CookTime", (short) cookTime);
        if (owner != null) {
            tag.putUUID("Owner", owner.owner());
            tag.putString("OwnerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        byte flags = tag.getByte("Flags");
        fastMode = (flags & 0x01) != 0;
        outputToEnderChest = (flags & 0x02) != 0;
        burnTimeRemaining = tag.getShort("BurnTimeRemaining");
        burnTimeFresh = tag.getShort("BurnTimeFresh");
        cookTime = tag.getShort("CookTime");
        if (tag.hasUUID("Owner")) {
            owner = new OwnerInfo(tag.getUUID("Owner"), tag.getString("OwnerName"), true);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        setOwner(player);
        return new EnderFurnaceMenu(id, playerInventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        super.writeClientSideData(menu, buffer);
        buffer.writeUtf(getOwnerName());
    }
}
