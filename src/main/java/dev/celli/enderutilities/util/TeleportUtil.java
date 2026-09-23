package dev.celli.enderutilities.util;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.util.ChunkLoading;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

public final class TeleportUtil {
    private TeleportUtil() {
    }

    public static boolean doesEntityStackHavePlayers(Entity entity) {
        Entity current = entity;
        while (current != null) {
            if (current instanceof Player) {
                return true;
            }
            current = current.getVehicle();
        }
        for (Entity passenger : EntityUtils.getBottomEntity(entity).getIndirectPassengers()) {
            if (passenger instanceof Player) {
                return true;
            }
        }
        return false;
    }

    public static boolean canTeleportEntity(Entity entity) {
        return !EntityUtils.doesEntityStackHaveBlacklistedEntities(entity);
    }

    public static Entity teleportEntityUsingModularItem(Entity entity, ItemStack stack, boolean allowMounts, boolean allowRiders) {
        return teleportUsingTarget(entity, dev.celli.enderutilities.item.module.ModularHelper.getSelectedTarget(stack), allowMounts, allowRiders);
    }

    public static Entity teleportUsingTarget(Entity entity, TargetLocation target, boolean allowMounts, boolean allowRiders) {
        return teleportUsingTarget(entity, target, true, allowMounts, allowRiders);
    }

    public static Entity teleportUsingTarget(Entity entity, TargetLocation target, boolean adjustTargetPosition, boolean allowMounts, boolean allowRiders) {
        if (target == null || entity == null || entity.level().isClientSide) {
            return null;
        }
        Vec3 dest = adjustTargetPosition ? adjustTargetPosition(target, entity) : target.exact();
        if (target.hasRotation()) {
            entity.setYRot(target.yaw());
            entity.setXRot(target.pitch());
        }
        return teleportEntity(entity, dest, target.dimension(), allowMounts, allowRiders, target.hasRotation(), target.yaw(), target.pitch());
    }

    public static Vec3 adjustTargetPosition(TargetLocation target, Entity entity) {
        Direction face = target.face();
        if (face == null) {
            return target.exact();
        }
        float widthAdj = entity.getBbWidth() / 2.0f;
        double x = target.exact().x + face.getStepX() * widthAdj;
        double y = target.exact().y;
        double z = target.exact().z + face.getStepZ() * widthAdj;
        if (face == Direction.DOWN) {
            y -= entity.getBbHeight();
        }
        return new Vec3(x, y, z);
    }

    public static Entity teleportEntity(
            Entity entityIn,
            Vec3 dest,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> destDim,
            boolean allowMounts,
            boolean allowRiders,
            boolean hasRotation,
            float yaw,
            float pitch
    ) {
        if (entityIn == null || entityIn.level().isClientSide) {
            return null;
        }
        if (!allowMounts && entityIn.isPassenger()) {
            return null;
        }
        if (!allowRiders && entityIn.isVehicle()) {
            return null;
        }
        if (!canTeleportEntity(entityIn)) {
            return null;
        }

        UUID uuidOriginal = entityIn.getUUID();
        Entity entity = EntityUtils.getBottomEntity(entityIn);
        boolean isOriginal = entity != entityIn;
        List<Entity> passengers = List.copyOf(entity.getPassengers());
        boolean ridden = entity.isVehicle();
        entity.ejectPassengers();

        float useYaw = hasRotation ? yaw : entity.getYRot();
        float usePitch = hasRotation ? pitch : entity.getXRot();
        Entity teleported = teleportSingle(entity, dest, destDim, useYaw, usePitch);
        if (teleported == null) {
            return null;
        }
        teleported.resetFallDistance();

        if (ridden) {
            for (Entity passenger : passengers) {
                Entity moved = teleportEntity(passenger, dest, destDim, allowMounts, allowRiders, hasRotation, yaw, pitch);
                if (moved != null) {
                    moved.startRiding(teleported, true);
                }
            }
        }

        if (isOriginal) {
            Entity match = findByUuid(teleported, uuidOriginal);
            if (match != null) {
                return match;
            }
        }
        return teleported;
    }

    private static Entity findByUuid(Entity root, UUID uuid) {
        if (root.getUUID().equals(uuid)) {
            return root;
        }
        for (Entity passenger : root.getPassengers()) {
            Entity found = findByUuid(passenger, uuid);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Entity teleportSingle(
            Entity entity,
            Vec3 dest,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> destDim,
            float yaw,
            float pitch
    ) {
        if (!entity.isAlive() || !canTeleportEntity(entity) || entity.level().isClientSide) {
            return null;
        }
        playTeleportEffects(entity);

        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ServerLevel destLevel = serverLevel.getServer().getLevel(destDim);
        if (destLevel == null) {
            return null;
        }
        destLevel.getChunk(net.minecraft.util.Mth.floor(dest.x) >> 4, net.minecraft.util.Mth.floor(dest.z) >> 4);
        ChunkLoading.getInstance().forceChunk(destLevel, net.minecraft.util.Mth.floor(dest.x) >> 4,
                net.minecraft.util.Mth.floor(dest.z) >> 4, ChunkLoading.TIMEOUT_TELEPORT);

        Entity teleported;
        if (destLevel == serverLevel) {
            if (entity instanceof ServerPlayer player) {
                player.teleportTo(player.serverLevel(), dest.x, dest.y, dest.z, Set.of(), yaw, pitch);
                player.resetFallDistance();
                teleported = player;
            } else {
                entity.teleportTo(dest.x, dest.y, dest.z);
                entity.setYRot(yaw);
                entity.setXRot(pitch);
                entity.resetFallDistance();
                teleported = entity;
            }
        } else {
            DimensionTransition transition = new DimensionTransition(
                    destLevel,
                    dest,
                    Vec3.ZERO,
                    yaw,
                    pitch,
                    DimensionTransition.DO_NOTHING
            );
            teleported = entity.changeDimension(transition);
        }

        if (teleported != null) {
            playTeleportEffects(teleported);
        }
        return teleported;
    }

    public static boolean teleportEntityRandomly(Entity entity, double maxDist) {
        if (!canTeleportEntity(entity) || entity.level().isClientSide) {
            return false;
        }
        var random = entity.level().random;
        maxDist = maxDist - (random.nextFloat() * maxDist / 2.0);
        var box = entity.getBoundingBox();
        for (int i = 0; i < 20; i++) {
            double deltaYaw = random.nextFloat() * 2.0 * Math.PI;
            double deltaPitch = random.nextFloat() * 0.5 * Math.PI;
            double x = entity.getX() + Math.cos(deltaPitch) * Math.cos(deltaYaw) * maxDist;
            double z = entity.getZ() + Math.cos(deltaPitch) * Math.sin(deltaYaw) * maxDist;
            double y = entity.getY() + Math.sin(deltaPitch) * maxDist;
            var moved = box.move(x - entity.getX(), y - entity.getY(), z - entity.getZ());
            if (entity.level().noCollision(entity, moved)) {
                return teleportEntity(
                        entity,
                        new Vec3(x, y, z),
                        entity.level().dimension(),
                        true,
                        true,
                        false,
                        entity.getYRot(),
                        entity.getXRot()
                ) != null;
            }
        }
        return false;
    }

    public static boolean teleportEntityWithProjectile(
            Entity entity,
            Entity projectile,
            net.minecraft.world.phys.HitResult hit,
            float teleportDamage,
            boolean allowMounts,
            boolean allowRiders
    ) {
        if (!canTeleportEntity(entity) || hit == null) {
            return false;
        }
        Vec3 dest = hit.getLocation();
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            var face = blockHit.getDirection();
            dest = dest.add(face.getStepX() * entity.getBbWidth() / 2.0, 0.0, face.getStepZ() * entity.getBbWidth() / 2.0);
            if (face == Direction.DOWN) {
                dest = dest.subtract(0.0, entity.getBbHeight(), 0.0);
            }
        }
        Entity moved = teleportEntity(
                entity,
                dest,
                projectile.level().dimension(),
                allowMounts,
                allowRiders,
                false,
                entity.getYRot(),
                entity.getXRot()
        );
        if (moved == null) {
            return false;
        }
        if (teleportDamage != 0.0F) {
            Entity bottom = EntityUtils.getBottomEntity(moved);
            if (bottom instanceof net.minecraft.world.entity.LivingEntity living) {
                living.hurt(living.damageSources().fall(), teleportDamage);
            }
        }
        moved.resetFallDistance();
        return true;
    }

    private static void playTeleportEffects(Entity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 0.5, entity.getZ(), 32, 0.5, 0.5, 0.5, 0.2);
            serverLevel.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }
}
