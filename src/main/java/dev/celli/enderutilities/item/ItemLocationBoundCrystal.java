package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.item.module.IKeyBound;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemLocationBoundCrystal extends ItemLinkCrystal implements IKeyBound {
    public ItemLocationBoundCrystal(Properties properties, int crystalType) {
        super(properties, crystalType);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (crystalType() == TYPE_PORTAL
                && !context.getLevel().getBlockState(context.getClickedPos()).is(dev.celli.enderutilities.registry.ModBlocks.PORTAL.get())) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide) {
            boolean adjust = crystalType() == TYPE_LOCATION;
            BlockPos pos = context.getClickedPos();
            ModularHelper.writeTargetToCrystal(
                    context.getItemInHand(),
                    player,
                    pos,
                    context.getClickedFace(),
                    context.getClickLocation().x - pos.getX(),
                    context.getClickLocation().y - pos.getY(),
                    context.getClickLocation().z - pos.getZ(),
                    adjust,
                    false
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            net.minecraft.world.InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (crystalType() == TYPE_PORTAL) {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                BlockPos pos = player.blockPosition();
                ModularHelper.writeTargetToCrystal(
                        stack,
                        player,
                        pos,
                        net.minecraft.core.Direction.UP,
                        player.getX() - pos.getX(),
                        player.getY() - pos.getY(),
                        player.getZ() - pos.getZ(),
                        crystalType() == TYPE_LOCATION,
                        true
                );
            }
            return ItemEnderUtilities.handledWithoutUseAnim(stack, level.isClientSide);
        }
        return net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT_ALT) && crystalType() == TYPE_LOCATION) {
            BlockPos pos = player.blockPosition();
            ModularHelper.writeTargetToCrystal(
                    stack,
                    player,
                    pos,
                    net.minecraft.core.Direction.UP,
                    player.getX() - pos.getX(),
                    player.getY() - pos.getY(),
                    player.getZ() - pos.getZ(),
                    true,
                    true
            );
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_ALT)) {
            OwnerInfo owner = stack.get(ModDataComponents.OWNER);
            if (owner != null && owner.isOwner(player)) {
                stack.set(ModDataComponents.OWNER, owner.withPublic(!owner.isPublic()));
                return true;
            }
        }
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        return EnderItemTooltips.namedWithTarget(super.getName(stack), EnderItemTooltips.getCrystalTargetDisplayName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean verbose = ClientAccess.isShiftDown();
        TargetLocation target = stack.get(ModDataComponents.TARGET);
        if (target == null) {
            EnderItemTooltips.addTranslatedTips(getDescriptionId() + ".tooltips", tooltip, verbose);
            List<Component> lines = new java.util.ArrayList<>();
            lines.add(Component.translatable("enderutilities.tooltip.item.notargetset").withStyle(ChatFormatting.GRAY));
            EnderItemTooltips.addFoldedLines(tooltip, lines);
        } else {
            List<Component> verboseLines = new java.util.ArrayList<>();
            EnderItemTooltips.addTargetLines(stack, target, verboseLines, true);
            if (!verbose && verboseLines.size() > 2) {
                List<Component> compact = new java.util.ArrayList<>();
                EnderItemTooltips.addTargetLines(stack, target, compact, false);
                if (!compact.isEmpty()) {
                    tooltip.add(compact.getFirst());
                }
                tooltip.add(Component.translatable("enderutilities.tooltip.item.holdshift"));
            } else {
                tooltip.addAll(verboseLines);
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
