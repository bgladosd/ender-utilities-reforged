package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.item.ItemEnderCapacitor;
import dev.celli.enderutilities.item.module.IModular;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.menu.EnderInfuserMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class EnderInfuserBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_MATERIAL = 0;
    public static final int SLOT_CAP_IN = 1;
    public static final int SLOT_CAP_OUT = 2;
    public static final int AMOUNT_PER_ENDERPEARL = 250;
    public static final int AMOUNT_PER_ENDEREYE = 500;
    public static final int ENDER_CHARGE_PER_MILLIBUCKET = 4;
    public static final int MAX_AMOUNT = 4000;

    private int amountStored;
    private int meltingProgress;
    private boolean isCharging;
    private int chargeableItemCapacity;
    private int chargeableItemStartingCharge;
    private int chargeableItemCurrentCharge;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return EnderInfuserBlockEntity.isItemValidForSlot(slot, stack);
        }
    };

    private final IItemHandler hopperHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == SLOT_CAP_OUT || !isItemValidForSlot(slot, stack)) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != SLOT_CAP_OUT) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != SLOT_CAP_OUT && isItemValidForSlot(slot, stack);
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> amountStored;
                case 1 -> meltingProgress;
                case 2 -> getChargeProgress();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> amountStored = value;
                case 1 -> meltingProgress = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public EnderInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENDER_INFUSER.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public IItemHandler getHopperHandler() {
        return hopperHandler;
    }

    public ContainerData getData() {
        return data;
    }

    public int getAmountStored() {
        return amountStored;
    }

    public int getMeltingProgress() {
        return meltingProgress;
    }

    public int getChargeProgress() {
        if (chargeableItemCapacity != chargeableItemStartingCharge) {
            return (chargeableItemCurrentCharge - chargeableItemStartingCharge) * 100
                    / (chargeableItemCapacity - chargeableItemStartingCharge);
        }
        return 0;
    }

    public static boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot == SLOT_CAP_IN) {
            return isChargeableInput(stack);
        }
        return slot == SLOT_MATERIAL && (stack.is(Items.ENDER_PEARL) || stack.is(Items.ENDER_EYE));
    }

    public static boolean isChargeableInput(ItemStack stack) {
        if (stack.getItem() instanceof ItemEnderCapacitor) {
            return true;
        }
        return stack.getItem() instanceof IModular
                && ModularHelper.getInstalledModuleCount(stack, ModuleType.ENDERCAPACITOR) > 0;
    }

    public void serverTick() {
        boolean dirty = false;
        ItemStack material = inventory.getStackInSlot(SLOT_MATERIAL);
        if (!material.isEmpty()) {
            int amount = 0;
            if (material.is(Items.ENDER_PEARL)) {
                amount = AMOUNT_PER_ENDERPEARL;
            } else if (material.is(Items.ENDER_EYE)) {
                amount = AMOUNT_PER_ENDEREYE;
            }
            if (amount > 0 && amount + amountStored <= MAX_AMOUNT) {
                meltingProgress += 2;
                if (meltingProgress >= 100) {
                    amountStored += amount;
                    meltingProgress = 0;
                    inventory.extractItem(SLOT_MATERIAL, 1, false);
                }
                dirty = true;
            }
        } else {
            meltingProgress = 0;
        }

        ItemStack inputStack = inventory.getStackInSlot(SLOT_CAP_IN);
        if (!inputStack.isEmpty()) {
            ItemStack chargeableStack = inputStack;
            ItemEnderCapacitor capacitor = null;
            boolean isModular = false;
            if (inputStack.getItem() instanceof ItemEnderCapacitor cap) {
                capacitor = cap;
            } else if (inputStack.getItem() instanceof IModular) {
                chargeableStack = ModularHelper.getSelectedModuleStack(inputStack, ModuleType.ENDERCAPACITOR);
                if (chargeableStack.getItem() instanceof ItemEnderCapacitor cap) {
                    capacitor = cap;
                    isModular = true;
                }
            }

            if (capacitor != null && amountStored > 0) {
                int charge = (amountStored >= 10 ? 10 : amountStored) * ENDER_CHARGE_PER_MILLIBUCKET;
                int filled = capacitor.addCharge(chargeableStack, charge, false);
                if (filled > 0) {
                    if (!isCharging) {
                        chargeableItemCapacity = capacitor.getCapacity(chargeableStack);
                        chargeableItemStartingCharge = capacitor.getCharge(chargeableStack);
                        chargeableItemCurrentCharge = chargeableItemStartingCharge;
                        isCharging = true;
                    }
                    if (filled < charge) {
                        charge = filled;
                    }
                    charge = capacitor.addCharge(chargeableStack, charge, true);
                    int used = (int) Math.ceil(charge / ENDER_CHARGE_PER_MILLIBUCKET);
                    amountStored -= used;
                    chargeableItemCurrentCharge += charge;
                    dirty = true;
                    if (isModular) {
                        ModularHelper.setSelectedModuleStack(inputStack, ModuleType.ENDERCAPACITOR, chargeableStack);
                    }
                }
            }

            if (capacitor != null && capacitor.getCharge(chargeableStack) >= capacitor.getCapacity(chargeableStack)) {
                isCharging = false;
                chargeableItemCurrentCharge = 0;
                chargeableItemStartingCharge = 0;
                chargeableItemCapacity = 0;
                ItemStack simulated = inventory.extractItem(SLOT_CAP_IN, 1, true);
                if (inventory.insertItem(SLOT_CAP_OUT, simulated, true).isEmpty()) {
                    inventory.insertItem(SLOT_CAP_OUT, inventory.extractItem(SLOT_CAP_IN, 1, false), false);
                    dirty = true;
                }
            }
        } else {
            isCharging = false;
            chargeableItemCurrentCharge = 0;
            chargeableItemStartingCharge = 0;
            chargeableItemCapacity = 0;
        }

        if (dirty) {
            setChanged();
        }
    }

    public void dropContents() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        CompoundTag fluid = new CompoundTag();
        fluid.putInt("Amount", amountStored);
        fluid.putString("FluidName", "ender");
        tag.put("Fluid", fluid);
        tag.putByte("Progress", (byte) meltingProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Fluid")) {
            amountStored = tag.getCompound("Fluid").getInt("Amount");
        }
        meltingProgress = tag.getByte("Progress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("enderutilities.container.ender_infuser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EnderInfuserMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }
}
