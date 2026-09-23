package dev.celli.enderutilities.item.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.celli.enderutilities.data.ModularItemData;
import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.data.StoredModule;
import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.item.ItemEnderCapacitor;
import dev.celli.enderutilities.item.ItemLinkCrystal;
import dev.celli.enderutilities.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ModularHelper {
    private ModularHelper() {
    }

    public static ModularItemData data(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MODULAR, ModularItemData.EMPTY);
    }

    public static void setData(ItemStack stack, ModularItemData data) {
        stack.set(ModDataComponents.MODULAR, data);
    }

    public static boolean moduleTypeEquals(ItemStack moduleStack, ModuleType type) {
        return !moduleStack.isEmpty()
                && moduleStack.getItem() instanceof IModule module
                && module.getModuleType(moduleStack) == type;
    }

    public static List<ItemStack> modulesOfType(ItemStack container, ModuleType type) {
        List<ItemStack> list = new ArrayList<>();
        for (StoredModule module : data(container).modules()) {
            if (moduleTypeEquals(module.stack(), type)) {
                list.add(module.stack().copy());
            }
        }
        return list;
    }

    public static int getInstalledModuleCount(ItemStack container, ModuleType type) {
        return modulesOfType(container, type).size();
    }

    public static int getMaxModuleTier(ItemStack container, ModuleType type) {
        int max = -1;
        for (ItemStack module : modulesOfType(container, type)) {
            if (module.getItem() instanceof IModule item) {
                if (item instanceof dev.celli.enderutilities.item.ItemEnderCore core && !core.isActive()) {
                    continue;
                }
                max = Math.max(max, item.getModuleTier(module));
            }
        }
        return max;
    }

    public static int getSelectedModuleTier(ItemStack container, ModuleType type) {
        ItemStack selected = getSelectedModuleStack(container, type);
        if (selected.isEmpty() || !(selected.getItem() instanceof IModule module)) {
            return -1;
        }
        return module.getModuleTier(selected);
    }

    public static int getClampedModuleSelection(ItemStack container, ModuleType type) {
        int selected = data(container).selectedLinkCrystal();
        int num = getInstalledModuleCount(container, type);
        if (selected >= num) {
            selected = num > 0 ? num - 1 : 0;
        }
        return Math.max(selected, 0);
    }

    public static ItemStack getSelectedModuleStack(ItemStack container, ModuleType type) {
        List<ItemStack> modules = modulesOfType(container, type);
        if (modules.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int selected = getClampedModuleSelection(container, type);
        if (selected >= modules.size()) {
            return ItemStack.EMPTY;
        }
        return modules.get(selected);
    }

    public static void setSelectedModuleStack(ItemStack container, ModuleType type, ItemStack newStack) {
        ModularItemData current = data(container);
        int seen = 0;
        int targetIndex = getClampedModuleSelection(container, type);
        ModularItemData next = current;
        for (StoredModule module : current.modules()) {
            if (moduleTypeEquals(module.stack(), type)) {
                if (seen == targetIndex) {
                    next = next.withSlot(module.slot(), newStack);
                    break;
                }
                seen++;
            }
        }
        setData(container, next);
    }

    public static boolean changeSelectedModule(ItemStack container, ModuleType type, boolean reverse) {
        int count = getInstalledModuleCount(container, type);
        if (count == 0) {
            return false;
        }
        int selected = getClampedModuleSelection(container, type);
        if (reverse) {
            selected--;
            if (selected < 0) {
                selected = count - 1;
            }
        } else {
            selected++;
            if (selected >= count) {
                selected = 0;
            }
        }
        setData(container, data(container).withSelectedLinkCrystal(selected));
        return true;
    }

    public static int addEnderCharge(ItemStack container, int amount, boolean doCharge) {
        if (container.isEmpty() || !(container.getItem() instanceof IModular)) {
            return 0;
        }
        ItemStack capStack = getSelectedModuleStack(container, ModuleType.ENDERCAPACITOR);
        if (!(capStack.getItem() instanceof ItemEnderCapacitor capacitor)) {
            return 0;
        }
        int added = capacitor.addCharge(capStack, amount, doCharge);
        if (doCharge && added > 0) {
            setSelectedModuleStack(container, ModuleType.ENDERCAPACITOR, capStack);
        }
        return added;
    }

    public static int getAvailableEnderCharge(ItemStack container) {
        if (!dev.celli.enderutilities.config.ModCommonConfig.USE_ENDER_CHARGE.getAsBoolean()) {
            return Integer.MAX_VALUE;
        }
        if (container.isEmpty() || !(container.getItem() instanceof IModular)) {
            return 0;
        }
        ItemStack capStack = getSelectedModuleStack(container, ModuleType.ENDERCAPACITOR);
        if (!(capStack.getItem() instanceof ItemEnderCapacitor capacitor)) {
            return 0;
        }
        return capacitor.getCharge(capStack);
    }

    public static boolean useEnderCharge(ItemStack container, int amount, boolean simulate) {
        if (!dev.celli.enderutilities.config.ModCommonConfig.USE_ENDER_CHARGE.getAsBoolean()) {
            return true;
        }
        if (container.isEmpty() || !(container.getItem() instanceof IModular)) {
            return false;
        }
        ItemStack capStack = getSelectedModuleStack(container, ModuleType.ENDERCAPACITOR);
        if (!(capStack.getItem() instanceof ItemEnderCapacitor capacitor)) {
            return false;
        }
        if (capacitor.useCharge(capStack, amount, false) < amount) {
            return false;
        }
        if (!simulate) {
            capacitor.useCharge(capStack, amount, true);
            setSelectedModuleStack(container, ModuleType.ENDERCAPACITOR, capStack);
        }
        return true;
    }

    public static TargetLocation getSelectedTarget(ItemStack container) {
        ItemStack crystal = getSelectedModuleStack(container, ModuleType.LINKCRYSTAL);
        return crystal.get(ModDataComponents.TARGET);
    }

    public static net.neoforged.neoforge.items.IItemHandler getBoundInventory(
            ItemStack container,
            net.minecraft.world.entity.player.Player player,
            int timeoutSeconds
    ) {
        TargetLocation target = getSelectedTarget(container);
        if (target == null || player == null || player.level().isClientSide) {
            return null;
        }
        if (!canAccessSelectedModule(container, ModuleType.LINKCRYSTAL, player)) {
            return null;
        }
        if (target.isEnderChest()) {
            return new net.neoforged.neoforge.items.wrapper.InvWrapper(player.getEnderChestInventory());
        }
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel server)) {
            return null;
        }
        net.minecraft.server.level.ServerLevel dest = server.getServer().getLevel(target.dimension());
        if (dest == null) {
            return null;
        }
        if (timeoutSeconds > 0) {
            dev.celli.enderutilities.util.ChunkLoading.getInstance().forceChunk(dest, target.pos(), timeoutSeconds, player.getUUID());
        }
        return dev.celli.enderutilities.util.InventoryHelper.inventoryAt(dest, target.pos(), target.face());
    }

    public static boolean canAccessSelectedModule(ItemStack container, ModuleType type, Player player) {
        ItemStack module = getSelectedModuleStack(container, type);
        if (module.isEmpty()) {
            return false;
        }
        OwnerInfo owner = module.get(ModDataComponents.OWNER);
        return owner == null || owner.canAccess(player);
    }

    public static boolean setTarget(
            ItemStack container,
            Player player,
            BlockPos pos,
            Direction side,
            double hitX,
            double hitY,
            double hitZ,
            boolean doHitOffset,
            boolean storeRotation
    ) {
        if (!canAccessSelectedModule(container, ModuleType.LINKCRYSTAL, player)) {
            return false;
        }
        ItemStack crystal = getSelectedModuleStack(container, ModuleType.LINKCRYSTAL);
        if (crystal.isEmpty()) {
            return false;
        }

        Vec3 exact;
        if (doHitOffset) {
            exact = new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ);
        } else {
            exact = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }

        BlockState state = player.level().getBlockState(pos);
        Optional<ResourceLocation> blockId = Optional.of(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()));

        TargetLocation target = new TargetLocation(
                player.level().dimension(),
                pos.immutable(),
                exact,
                side,
                blockId,
                storeRotation,
                player.getYRot(),
                player.getXRot()
        );
        crystal.set(ModDataComponents.TARGET, target);
        if (crystal.get(ModDataComponents.OWNER) == null) {
            crystal.set(ModDataComponents.OWNER, OwnerInfo.of(player, true));
        }
        setSelectedModuleStack(container, ModuleType.LINKCRYSTAL, crystal);
        return true;
    }

    public static void setTargetFromPlayer(ItemStack container, Player player, boolean storeRotation) {
        BlockPos pos = player.blockPosition();
        double hitX = player.getX() - pos.getX();
        double hitY = player.getY() - pos.getY();
        double hitZ = player.getZ() - pos.getZ();
        boolean adjust = getSelectedModuleTier(container, ModuleType.LINKCRYSTAL) == ItemLinkCrystal.TYPE_LOCATION;
        setTarget(container, player, pos, Direction.UP, hitX, hitY, hitZ, adjust, storeRotation);
    }

    public static void writeTargetToCrystal(
            ItemStack crystal,
            Player player,
            BlockPos pos,
            Direction side,
            double hitX,
            double hitY,
            double hitZ,
            boolean doHitOffset,
            boolean storeRotation
    ) {
        OwnerInfo owner = crystal.get(ModDataComponents.OWNER);
        if (owner != null && !owner.canAccess(player)) {
            return;
        }
        Vec3 exact = doHitOffset
                ? new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ)
                : new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        BlockState state = player.level().getBlockState(pos);
        crystal.set(ModDataComponents.TARGET, new TargetLocation(
                player.level().dimension(),
                pos.immutable(),
                exact,
                side,
                Optional.of(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock())),
                storeRotation,
                player.getYRot(),
                player.getXRot()
        ));
        if (crystal.get(ModDataComponents.OWNER) == null) {
            crystal.set(ModDataComponents.OWNER, OwnerInfo.of(player, true));
        }
    }
}
