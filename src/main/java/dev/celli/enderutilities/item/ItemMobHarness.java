package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.data.HarnessTarget;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EntityUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

public class ItemMobHarness extends ItemEnderUtilities {
    public ItemMobHarness(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean hasTarget(ItemStack stack) {
        return stack.has(ModDataComponents.HARNESS_TARGET);
    }

    @Override
    public Component getName(ItemStack stack) {
        HarnessTarget target = stack.get(ModDataComponents.HARNESS_TARGET);
        if (target == null) {
            return super.getName(stack);
        }
        return super.getName(stack).copy().append(" ").append(Component.literal(target.name()).withStyle(ChatFormatting.GREEN));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.isShiftKeyDown()) {
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, true);
            if (hit.getType() != HitResult.Type.ENTITY && player.getXRot() > 80.0F) {
                stack.remove(ModDataComponents.HARNESS_TARGET);
            }
        }
        return ItemEnderUtilities.handledWithoutUseAnim(stack, false);
    }

    public boolean handleInteraction(ItemStack stack, Player player, Entity entity) {
        if (!player.isShiftKeyDown()) {
            player.startRiding(entity, true);
            return true;
        }
        if (hasTarget(stack)) {
            mountTarget(stack, player, entity);
        } else if (player.getXRot() < -80.0F && player.isVehicle()) {
            player.ejectPassengers();
        } else if (entity.isPassenger()) {
            entity.stopRiding();
        } else {
            stack.set(ModDataComponents.HARNESS_TARGET, new HarnessTarget(
                    entity.getUUID(),
                    entity.getName().getString(),
                    (byte) (entity instanceof Player ? 2 : 1)
            ));
        }
        return true;
    }

    private void mountTarget(ItemStack stack, Player player, Entity targetEntity) {
        HarnessTarget stored = stack.get(ModDataComponents.HARNESS_TARGET);
        if (stored == null) {
            return;
        }
        if (stored.uuid().equals(targetEntity.getUUID())) {
            EntityUtils.unmountFirstRider(player);
            targetEntity.startRiding(player, true);
            stack.remove(ModDataComponents.HARNESS_TARGET);
            return;
        }
        Entity storedEntity = null;
        double r = 64.0;
        if (stored.mode() == 1) {
            AABB box = player.getBoundingBox().inflate(r);
            List<Entity> entities = player.level().getEntities(player, box);
            storedEntity = EntityUtils.findEntityByUUID(entities, stored.uuid());
        } else if (stored.mode() == 2) {
            storedEntity = player.level().getPlayerByUUID(stored.uuid());
        }
        if (storedEntity != null && storedEntity.level().dimension().equals(player.level().dimension())) {
            if (!EntityUtils.doesEntityStackContainEntity(storedEntity, targetEntity)) {
                storedEntity.startRiding(targetEntity, true);
                stack.remove(ModDataComponents.HARNESS_TARGET);
            }
        } else if (storedEntity == null && !player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("enderutilities.chat.message.mobharness.targetnotfoundoroutofrange"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        HarnessTarget target = stack.get(ModDataComponents.HARNESS_TARGET);
        if (target == null) {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.notlinked"));
        } else {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.linked")
                    .append(": ")
                    .append(Component.literal(target.name()).withStyle(ChatFormatting.GREEN)));
        }
    }
}
