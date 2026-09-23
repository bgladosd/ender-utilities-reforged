package dev.celli.enderutilities.entity;

import java.util.UUID;

import dev.celli.enderutilities.registry.ModEntities;
import dev.celli.enderutilities.registry.ModItems;
import dev.celli.enderutilities.util.EntityUtils;
import dev.celli.enderutilities.util.TeleportUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class EntityEnderPearlReusable extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> ELITE =
            SynchedEntityData.defineId(EntityEnderPearlReusable.class, EntityDataSerializers.BOOLEAN);

    public float teleportDamage = 2.0F;
    public boolean canPickUp = true;
    public boolean isElite;
    private UUID throwerUUID;

    public EntityEnderPearlReusable(EntityType<? extends EntityEnderPearlReusable> type, Level level) {
        super(type, level);
    }

    public EntityEnderPearlReusable(Level level, LivingEntity thrower, boolean elite) {
        super(ModEntities.ENDER_PEARL_REUSABLE.get(), thrower, level);
        this.canPickUp = !(thrower instanceof Player player) || !player.getAbilities().instabuild;
        this.isElite = elite;
        this.throwerUUID = thrower.getUUID();
        setOwner(thrower);
        getEntityData().set(ELITE, elite);
        setItem(new ItemStack(elite ? ModItems.ENDER_PEARL_REUSABLE_ELITE.get() : ModItems.ENDER_PEARL_REUSABLE.get()));
        this.teleportDamage = elite ? 1.0F : 2.0F;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ELITE, false);
    }

    public boolean isElitePearl() {
        return this.isElite || this.entityData.get(ELITE);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ENDER_PEARL_REUSABLE.get();
    }

    @Override
    public ItemStack getItem() {
        ItemStack stack = super.getItem();
        if (!stack.isEmpty()) {
            return stack;
        }
        return new ItemStack(isElitePearl() ? ModItems.ENDER_PEARL_REUSABLE_ELITE.get() : ModItems.ENDER_PEARL_REUSABLE.get());
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return isElitePearl() && getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return isElitePearl();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0, Math.max(0.25, dimensions.height() * 0.5), 0.0);
    }

    @Override
    public void onBelowWorld() {
        if (!level().isClientSide && !returnToPlayersInventory()) {
            dropAsItem();
        }
        super.onBelowWorld();
    }

    @Override
    public void tick() {
        if (!level().isClientSide && isElitePearl() && tickCount > 2 && !isVehicle()) {
            if (!returnToPlayersInventory()) {
                dropAsItem();
            }
            discard();
            return;
        }
        super.tick();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity thrower = getThrower();
        if (thrower != null && EntityUtils.doesEntityStackContainEntity(target, thrower)) {
            return false;
        }
        if (target instanceof ServerPlayer player && player.isSpectator()) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void onHit(HitResult result) {
        if (level().isClientSide) {
            discard();
            return;
        }

        Entity thrower = getThrower();
        if (thrower == null) {
            if (canPickUp) {
                dropAsItem();
            }
            discard();
            return;
        }

        if (result.getType() == HitResult.Type.ENTITY && result instanceof EntityHitResult entityHit) {
            if (EntityUtils.doesEntityStackContainEntity(entityHit.getEntity(), thrower)) {
                return;
            }
            if (entityHit.getEntity() instanceof ServerPlayer player && player.isSpectator()) {
                return;
            }
            if (entityHit.getEntity() instanceof LivingEntity living) {
                living.hurt(damageSources().thrown(this, thrower), 0.0F);
            }
        } else if (result.getType() == HitResult.Type.BLOCK && result instanceof BlockHitResult blockHit) {
            BlockState state = level().getBlockState(blockHit.getBlockPos());
            if (state.getCollisionShape(level(), blockHit.getBlockPos(), CollisionContext.empty()).isEmpty()) {
                return;
            }
        }

        if (!isElitePearl()) {
            Entity bottom = thrower.getRootVehicle();
            if (bottom instanceof EntityEnderPearlReusable && bottom.isVehicle()) {
                bottom.ejectPassengers();
            }
            TeleportUtil.teleportEntityWithProjectile(thrower, this, result, teleportDamage, true, true);
        } else if (isVehicle()) {
            Entity passenger = getPassengers().getFirst();
            ejectPassengers();
            TeleportUtil.teleportEntityWithProjectile(passenger, this, result, teleportDamage, true, true);
        }

        if (!returnToPlayersInventory()) {
            dropAsItem();
        }
        discard();
    }

    public Entity getThrower() {
        if (throwerUUID != null && level() instanceof ServerLevel serverLevel) {
            Entity player = serverLevel.getServer().getPlayerList().getPlayer(throwerUUID);
            if (player != null) {
                return player;
            }
            Entity byId = serverLevel.getEntity(throwerUUID);
            if (byId != null) {
                return byId;
            }
        }
        return getOwner();
    }

    public boolean returnToPlayersInventory() {
        if (!canPickUp || level().isClientSide) {
            return true;
        }
        Entity thrower = getThrower();
        ItemStack stack = getItem().copyWithCount(1);
        if (thrower instanceof ServerPlayer player && player.isAlive()) {
            if (!player.getInventory().add(stack)) {
                return false;
            }
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastChanges();
            }
            return true;
        }
        return false;
    }

    public void dropAsItem() {
        ItemStack stack = getItem().copyWithCount(1);
        ItemEntity item = new ItemEntity(level(), getX(), getY(), getZ(), stack);
        item.setDeltaMovement(
                0.05 * level().random.nextGaussian(),
                0.05 * level().random.nextGaussian() + 0.2,
                0.05 * level().random.nextGaussian()
        );
        item.setDefaultPickUpDelay();
        level().addFreshEntity(item);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Elite", isElitePearl());
        tag.putBoolean("CanPickUp", canPickUp);
        tag.putFloat("TeleportDamage", teleportDamage);
        if (throwerUUID != null) {
            tag.putUUID("Thrower", throwerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.isElite = tag.getBoolean("Elite");
        getEntityData().set(ELITE, this.isElite);
        canPickUp = !tag.contains("CanPickUp") || tag.getBoolean("CanPickUp");
        teleportDamage = tag.contains("TeleportDamage") ? tag.getFloat("TeleportDamage") : (isElitePearl() ? 1.0F : 2.0F);
        if (tag.hasUUID("Thrower")) {
            throwerUUID = tag.getUUID("Thrower");
        }
    }
}
