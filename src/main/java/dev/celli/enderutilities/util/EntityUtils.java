package dev.celli.enderutilities.util;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class EntityUtils {
    private static final Set<ResourceLocation> TELEPORT_BLACKLIST = Set.of(
            ResourceLocation.withDefaultNamespace("ender_dragon"),
            ResourceLocation.withDefaultNamespace("ender_crystal"),
            ResourceLocation.withDefaultNamespace("end_crystal"),
            ResourceLocation.withDefaultNamespace("wither")
    );

    private EntityUtils() {
    }

    public static <T extends Entity> T findEntityByUUID(List<T> list, UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (T entity : list) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

    public static boolean doesEntityStackContainEntity(Entity entity, Entity entityInStack) {
        if (entity == null || entityInStack == null) {
            return false;
        }
        return doesEntityStackContainEntity(entity, entityInStack, true);
    }

    private static boolean doesEntityStackContainEntity(Entity entity, Entity entityInStack, boolean startFromBottom) {
        if (startFromBottom) {
            entityInStack = getBottomEntity(entityInStack);
        }
        if (entity == entityInStack) {
            return true;
        }
        if (entityInStack.isVehicle()) {
            for (Entity passenger : entityInStack.getPassengers()) {
                if (passenger == entity || doesEntityStackContainEntity(entity, passenger, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isEntityCollidingWithBlock(Level level, Entity entity, net.minecraft.world.level.block.Block block) {
        AABB box = entity.getBoundingBox();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.floor(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (level.getBlockState(cursor.set(x, y, z)).is(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static net.minecraft.core.Direction getLookingDirection(Entity entity) {
        return net.minecraft.core.Direction.orderedByNearest(entity)[0];
    }

    public static net.minecraft.core.Direction getHorizontalLookingDirection(Entity entity) {
        return entity.getDirection();
    }

    public static Entity getTopEntity(Entity entity) {
        Entity current = entity;
        while (current != null && current.isVehicle() && !current.getPassengers().isEmpty()) {
            current = current.getPassengers().getFirst();
        }
        return current;
    }

    public static boolean unmountFirstRider(Entity entity) {
        if (entity != null && entity.isVehicle() && !entity.getPassengers().isEmpty()) {
            entity.getPassengers().getFirst().stopRiding();
            return true;
        }
        return false;
    }

    public static boolean spawnEnderCrystal(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return false;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (WorldUtils.isEndDimension(level)
                && (level.getBlockState(pos).is(Blocks.OBSIDIAN) || level.getBlockState(pos).is(Blocks.BEDROCK))) {
            AABB box = new AABB(x - 1.0, y - 1.0, z - 1.0, x + 1.0, y + 1.0, z + 1.0);
            if (!level.getEntitiesOfClass(EndCrystal.class, box).isEmpty()) {
                return false;
            }
            for (int by = y - 5; by <= y; by++) {
                for (int bx = x - 1; bx <= x + 1; bx++) {
                    for (int bz = z - 1; bz <= z + 1; bz++) {
                        if (!level.getBlockState(new BlockPos(bx, by, bz)).is(Blocks.OBSIDIAN)) {
                            return false;
                        }
                    }
                }
            }
            level.explode(null, x + 0.5, y + 1.0, z + 0.5, 3.0F, Level.ExplosionInteraction.BLOCK);
            spawnCrystal(level, x + 0.5, y + 1.0, z + 0.5);
            return true;
        }
        if (!WorldUtils.isEndDimension(level)) {
            spawnCrystal(level, x + 0.5, y + 2.0, z + 0.5);
            return true;
        }
        return false;
    }

    private static void spawnCrystal(Level level, double x, double y, double z) {
        EndCrystal crystal = EntityType.END_CRYSTAL.create(level);
        if (crystal == null) {
            return;
        }
        crystal.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(crystal);
    }

    public static Entity getBottomEntity(Entity entity) {
        Entity current = entity;
        while (current.getVehicle() != null) {
            current = current.getVehicle();
        }
        return current;
    }

    public static boolean doesEntityStackHaveBlacklistedEntities(Entity entity) {
        return doesEntityStackHaveBlacklistedEntities(entity, true);
    }

    private static boolean doesEntityStackHaveBlacklistedEntities(Entity entity, boolean startFromBottom) {
        if (isBlacklistedForTeleport(entity)) {
            return true;
        }
        if (startFromBottom) {
            entity = getBottomEntity(entity);
        }
        if (entity.isVehicle()) {
            for (Entity passenger : entity.getPassengers()) {
                if (isBlacklistedForTeleport(passenger) || doesEntityStackHaveBlacklistedEntities(passenger, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isBlacklistedForTeleport(Entity entity) {
        return TELEPORT_BLACKLIST.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    public static boolean applyMobPersistence(Mob living) {
        if (living.isPersistenceRequired()) {
            return false;
        }
        boolean canDespawn = living instanceof Enemy
                && !(living instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
                && !(living instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                || living instanceof WaterAnimal
                || living instanceof TamableAnimal tamable && !tamable.isTame();
        if (!canDespawn) {
            canDespawn = living.removeWhenFarAway(16384.0);
        }
        if (!canDespawn) {
            return false;
        }
        living.setPersistenceRequired();
        living.level().playSound(null, living.blockPosition(), dev.celli.enderutilities.registry.ModSounds.JAILER.get(),
                net.minecraft.sounds.SoundSource.MASTER, 0.8f, 1.2f);
        return true;
    }
}
