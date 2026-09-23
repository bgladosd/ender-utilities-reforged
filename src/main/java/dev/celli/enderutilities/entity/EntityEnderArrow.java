package dev.celli.enderutilities.entity;

import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.item.ItemEnderBow;
import dev.celli.enderutilities.registry.ModEntities;
import dev.celli.enderutilities.registry.ModItems;
import dev.celli.enderutilities.util.EntityUtils;
import dev.celli.enderutilities.util.TeleportUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class EntityEnderArrow extends AbstractArrow {
    public byte tpMode;
    public TargetLocation tpTarget;
    public boolean applyPersistence;
    public float teleportDamage = 2.0F;

    public EntityEnderArrow(EntityType<? extends EntityEnderArrow> type, Level level) {
        super(type, level);
    }

    public EntityEnderArrow(Level level, LivingEntity shooter, float velocity) {
        super(ModEntities.ENDER_ARROW.get(), shooter, level, new ItemStack(ModItems.ENDER_ARROW.get()), null);
        shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, velocity, 1.0F);
        if (shooter instanceof Player player && player.getAbilities().instabuild) {
            pickup = Pickup.CREATIVE_ONLY;
        }
    }

    public void setTpMode(byte mode) {
        this.tpMode = mode;
    }

    public void setTpTarget(TargetLocation target) {
        this.tpTarget = target;
    }

    public void setPersistence(boolean value) {
        this.applyPersistence = value;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.ENDER_ARROW.get());
    }

    @Override
    protected void onHit(HitResult result) {
        Entity shooter = getOwner();
        if (tpMode == ItemEnderBow.BOW_MODE_TP_SELF) {
            if (!level().isClientSide && shooter != null) {
                TeleportUtil.teleportEntityWithProjectile(shooter, this, result, teleportDamage, true, true);
                discard();
            }
            return;
        }
        if (tpMode == ItemEnderBow.BOW_MODE_TP_TARGET && result.getType() == HitResult.Type.ENTITY && result instanceof EntityHitResult entityHit) {
            Entity hit = entityHit.getEntity();
            if (shooter != null && !EntityUtils.doesEntityStackContainEntity(hit, shooter)
                    && !EntityUtils.doesEntityStackHaveBlacklistedEntities(hit)
                    && (ModCommonConfig.ENDER_BOW_ALLOW_PLAYERS.getAsBoolean() || !TeleportUtil.doesEntityStackHavePlayers(hit))) {
                if (!level().isClientSide && tpTarget != null) {
                    if (applyPersistence && hit instanceof Mob mob) {
                        EntityUtils.applyMobPersistence(mob);
                    }
                    TeleportUtil.teleportUsingTarget(hit, tpTarget, true, true);
                    discard();
                    return;
                }
            } else {
                setDeltaMovement(getDeltaMovement().scale(-0.1));
                setYRot(getYRot() + 180.0F);
                yRotO += 180.0F;
                return;
            }
        }
        super.onHit(result);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (tpMode == ItemEnderBow.BOW_MODE_TP_TARGET || tpMode == ItemEnderBow.BOW_MODE_TP_SELF) {
            return;
        }
        super.onHitEntity(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (tpMode == ItemEnderBow.BOW_MODE_TP_SELF) {
            return;
        }
        super.onHitBlock(result);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("tpMode", tpMode);
        tag.putBoolean("Persistence", applyPersistence);
        tag.putFloat("TeleportDamage", teleportDamage);
        if (tpTarget != null) {
            tag.put("TpTarget", TargetLocation.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, tpTarget)
                    .result().orElse(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tpMode = tag.getByte("tpMode");
        applyPersistence = tag.getBoolean("Persistence");
        teleportDamage = tag.contains("TeleportDamage") ? tag.getFloat("TeleportDamage") : 2.0F;
        if (tag.contains("TpTarget")) {
            tpTarget = TargetLocation.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("TpTarget")).result().orElse(null);
        }
    }
}
