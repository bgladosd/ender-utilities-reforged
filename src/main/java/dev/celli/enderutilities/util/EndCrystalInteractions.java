package dev.celli.enderutilities.util;

import dev.celli.enderutilities.item.ItemEnderCapacitor;
import dev.celli.enderutilities.item.ItemEnderCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class EndCrystalInteractions {
    private EndCrystalInteractions() {
    }

    public static boolean tryUseHeldItem(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !WorldUtils.isEndDimension(player.level()) || findLookedAtCrystal(player) == null) {
            return false;
        }
        if (stack.getItem() instanceof ItemEnderCapacitor capacitor) {
            if (!player.level().isClientSide) {
                capacitor.addCharge(stack, capacitor.getCapacity(stack) >> 2, true);
                sync(player);
            }
            return true;
        }
        if (stack.getItem() instanceof ItemEnderCore core && !core.isActive()) {
            player.setItemInHand(hand, core.activate(stack));
            if (!player.level().isClientSide) {
                sync(player);
            }
            return true;
        }
        return false;
    }

    public static EndCrystal findLookedAtCrystal(Player player) {
        double range = Math.max(player.blockInteractionRange(), player.entityInteractionRange());
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(range));
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                search,
                entity -> entity instanceof EndCrystal && entity.isAlive(),
                range * range
        );
        return hit != null && hit.getEntity() instanceof EndCrystal crystal ? crystal : null;
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.broadcastChanges();
        }
    }
}
