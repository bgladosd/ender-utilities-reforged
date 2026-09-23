package dev.celli.enderutilities.entity;

import dev.celli.enderutilities.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EntityChair extends Entity {
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(EntityChair.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT =
            SynchedEntityData.defineId(EntityChair.class, EntityDataSerializers.FLOAT);

    public EntityChair(EntityType<? extends EntityChair> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setInvulnerable(true);
    }

    public EntityChair(Level level) {
        this(ModEntities.CHAIR.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WIDTH, 0.5F);
        builder.define(HEIGHT, 0.75F);
    }

    public void setChairSize(float width, float height) {
        entityData.set(WIDTH, width);
        entityData.set(HEIGHT, height);
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        return EntityDimensions.fixed(entityData.get(WIDTH), entityData.get(HEIGHT));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (WIDTH.equals(key) || HEIGHT.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 4;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide && !getPassengers().contains(player)) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger) {
        return passenger.position();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Width") && tag.contains("Height")) {
            setChairSize(tag.getFloat("Width"), tag.getFloat("Height"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Width", entityData.get(WIDTH));
        tag.putFloat("Height", entityData.get(HEIGHT));
    }
}
