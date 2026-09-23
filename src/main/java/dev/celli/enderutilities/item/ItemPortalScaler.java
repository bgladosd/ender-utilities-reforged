package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.data.PortalScale;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EntityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ItemPortalScaler extends ItemModular {
    public static final int ENDER_CHARGE_COST_PORTAL_ACTIVATION = 500;
    public static final float TELEPORTATION_EC_COST = 0.013F;

    public ItemPortalScaler(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && EntityUtils.isEntityCollidingWithBlock(player.level(), player, Blocks.NETHER_PORTAL)) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
            if (!level.isClientSide && player != null && level.mayInteract(player, pos)) {
                level.destroyBlock(pos, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos spawnAt = pos.relative(context.getClickedFace());
        if (level.getBlockState(pos).is(Blocks.OBSIDIAN)
                && level.isEmptyBlock(spawnAt)
                && player != null
                && level.mayInteract(player, spawnAt)
                && ModularHelper.useEnderCharge(stack, ENDER_CHARGE_COST_PORTAL_ACTIVATION, true)
                && net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(level, spawnAt, context.getClickedFace())) {
            ModularHelper.useEnderCharge(stack, ENDER_CHARGE_COST_PORTAL_ACTIVATION, false);
            level.setBlock(spawnAt, net.minecraft.world.level.block.BaseFireBlock.getState(level, spawnAt), 11);
            level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.MASTER, 0.8F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!EntityUtils.isEntityCollidingWithBlock(level, player, Blocks.NETHER_PORTAL)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            usePortalWithPortalScaler(stack, player);
        }
        return ItemEnderUtilities.handledWithoutUseAnim(stack, level.isClientSide);
    }

    public boolean usePortalWithPortalScaler(ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel source) || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        boolean overworld = source.dimension() == Level.OVERWORLD;
        boolean nether = source.dimension() == Level.NETHER;
        if ((!overworld && !nether) || !itemHasScaleFactor(stack)) {
            return false;
        }
        ServerLevel dest = source.getServer().getLevel(overworld ? Level.NETHER : Level.OVERWORLD);
        if (dest == null) {
            return false;
        }
        Vec3 normalDest = getNormalDestinationPosition(player, dest);
        Vec3 posDest = getDestinationPosition(stack, player, dest);
        int cost = getTeleportCost(normalDest, posDest);
        if (!ModularHelper.useEnderCharge(stack, cost, true)) {
            player.displayClientMessage(Component.translatable("enderutilities.chat.message.notenoughendercharge"), true);
            return false;
        }
        ModularHelper.useEnderCharge(stack, cost, false);
        serverPlayer.changeDimension(new DimensionTransition(
                dest,
                posDest,
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND
        ));
        return true;
    }

    public Vec3 getDestinationPosition(ItemStack stack, Player player, ServerLevel dest) {
        PortalScale scale = getScale(stack);
        double dScaleX = scale.x();
        double dScaleY = scale.y();
        double dScaleZ = scale.z();
        if (scale.x() < 0) {
            dScaleX = -1.0 / scale.x();
        }
        if (scale.y() < 0) {
            dScaleY = -1.0 / scale.y();
        }
        if (scale.z() < 0) {
            dScaleZ = -1.0 / scale.z();
        }
        if (dest.dimension() == Level.NETHER) {
            dScaleX = 1.0 / dScaleX;
            dScaleY = 1.0 / dScaleY;
            dScaleZ = 1.0 / dScaleZ;
        }
        return scaledClamped(player.position(), dest, dScaleX, dScaleY, dScaleZ);
    }

    public Vec3 getNormalDestinationPosition(Player player, ServerLevel dest) {
        double scale = dest.dimension() == Level.OVERWORLD ? 8.0 : 1.0 / 8.0;
        return scaledClamped(player.position(), dest, scale, 1.0, scale);
    }

    private static Vec3 scaledClamped(Vec3 pos, ServerLevel dest, double sx, double sy, double sz) {
        double x = Mth.clamp(pos.x * sx, dest.getWorldBorder().getMinX() + 16, dest.getWorldBorder().getMaxX() - 16);
        double y = Mth.clamp(pos.y * sy, dest.getMinBuildHeight() + 1, dest.getMaxBuildHeight() - 2);
        double z = Mth.clamp(pos.z * sz, dest.getWorldBorder().getMinZ() + 16, dest.getWorldBorder().getMaxZ() - 16);
        return new Vec3(x, y, z);
    }

    public int getTeleportCost(Vec3 pos1, Vec3 pos2) {
        return (int) (TELEPORTATION_EC_COST * pos1.distanceTo(pos2));
    }

    public PortalScale getScale(ItemStack stack) {
        ItemStack card = ModularHelper.getSelectedModuleStack(stack, ModuleType.MEMORY_CARD_MISC);
        return card.getOrDefault(ModDataComponents.PORTAL_SCALE, PortalScale.DEFAULT);
    }

    public boolean itemHasScaleFactor(ItemStack stack) {
        ItemStack card = ModularHelper.getSelectedModuleStack(stack, ModuleType.MEMORY_CARD_MISC);
        return !card.isEmpty() && card.has(ModDataComponents.PORTAL_SCALE);
    }

    public void changeCoordinateScaleFactor(ItemStack stack, Player player, int amount) {
        ItemStack moduleStack = ModularHelper.getSelectedModuleStack(stack, ModuleType.MEMORY_CARD_MISC);
        if (moduleStack.isEmpty()) {
            return;
        }
        PortalScale current = moduleStack.getOrDefault(ModDataComponents.PORTAL_SCALE, PortalScale.DEFAULT);
        Direction facing = EntityUtils.getLookingDirection(player);
        int x = hop(current.x() + Math.abs(facing.getStepX()) * amount, Math.abs(facing.getStepX()) * amount);
        int y = hop(current.y() + Math.abs(facing.getStepY()) * amount, Math.abs(facing.getStepY()) * amount);
        int z = hop(current.z() + Math.abs(facing.getStepZ()) * amount, Math.abs(facing.getStepZ()) * amount);
        moduleStack.set(ModDataComponents.PORTAL_SCALE, new PortalScale(
                (byte) Mth.clamp(x, -64, 64),
                (byte) Mth.clamp(y, -64, 64),
                (byte) Mth.clamp(z, -64, 64)
        ));
        ModularHelper.setSelectedModuleStack(stack, ModuleType.MEMORY_CARD_MISC, moduleStack);
    }

    private static int hop(int value, int step) {
        if (value == -1 || value == 0) {
            return value + step * 2;
        }
        return value;
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack card = ModularHelper.getSelectedModuleStack(stack, ModuleType.MEMORY_CARD_MISC);
        if (card.isEmpty()) {
            return super.getName(stack);
        }
        PortalScale scale = card.getOrDefault(ModDataComponents.PORTAL_SCALE, PortalScale.DEFAULT);
        if (!card.has(ModDataComponents.PORTAL_SCALE)) {
            return super.getName(stack);
        }
        return Component.literal(super.getName(stack).getString() + String.format(" x: %s y: %s z: %s",
                formatScale(scale.x()), formatScale(scale.y()), formatScale(scale.z())));
    }

    private static String formatScale(byte value) {
        return value < 0 ? "1/" + (-value) : String.valueOf(value);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ItemStack card = ModularHelper.getSelectedModuleStack(stack, ModuleType.MEMORY_CARD_MISC);
        if (card.isEmpty()) {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.nomemorycards"));
        } else if (card.has(ModDataComponents.PORTAL_SCALE)) {
            PortalScale scale = card.get(ModDataComponents.PORTAL_SCALE);
            tooltip.add(Component.literal(String.format("x: %s y: %s z: %s",
                    formatScale(scale.x()), formatScale(scale.y()), formatScale(scale.z()))));
        } else {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.nodata"));
        }
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL)) {
            return ModularHelper.changeSelectedModule(
                    stack,
                    ModuleType.MEMORY_CARD_MISC,
                    EnumKey.keypressActionIsReversed(key) || EnumKey.keypressContainsShift(key)
            );
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT_ALT, HotKeys.MOD_CTRL)) {
            changeCoordinateScaleFactor(stack, player, EnumKey.keypressContainsControl(key) ? 1 : -1);
            return true;
        }
        if (EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT_ALT)) {
            changeCoordinateScaleFactor(stack, player, EnumKey.keypressActionIsReversed(key) ? 1 : -1);
            return true;
        }
        return false;
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 5;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType type) {
        return switch (type) {
            case ENDERCAPACITOR -> 1;
            case MEMORY_CARD_MISC -> 4;
            default -> 0;
        };
    }

    @Override
    public int getMaxModules(ItemStack container, ItemStack moduleStack) {
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof IModule module)) {
            return 0;
        }
        return getMaxModules(container, module.getModuleType(moduleStack));
    }
}
