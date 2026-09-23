package dev.celli.enderutilities.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.item.tool.ItemEnderSword;
import dev.celli.enderutilities.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;

public class EntityEndermanFighter extends Monster {
    private static final EntityDataAccessor<Boolean> RAGING =
            SynchedEntityData.defineId(EntityEndermanFighter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SCREAMING =
            SynchedEntityData.defineId(EntityEndermanFighter.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation ATTACK_BOOST_ID =
            ResourceLocation.fromNamespaceAndPath("enderutilities", "fighter_attacking");
    private static final AttributeModifier ATTACK_BOOST =
            new AttributeModifier(ATTACK_BOOST_ID, 0.2, AttributeModifier.Operation.ADD_VALUE);

    private UUID assignedTarget;
    private UUID revengeTarget;
    private boolean beingControlled;

    public EntityEndermanFighter(EntityType<? extends EntityEndermanFighter> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RAGING, false);
        builder.define(SCREAMING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(6, new AIDespawn(200));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new AIAttackAssigned(true));
        targetSelector.addGoal(2, new AIAttackRevenge(false));
        targetSelector.addGoal(3, new AIAttackClosestPlayer(false));
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    public boolean isRaging() {
        return entityData.get(RAGING);
    }

    public boolean isScreaming() {
        return entityData.get(SCREAMING);
    }

    public boolean isBeingControlled() {
        return beingControlled;
    }

    public void setAssignedTarget(LivingEntity target) {
        assignedTarget = target.getUUID();
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (target == null) {
            entityData.set(SCREAMING, false);
            speed.removeModifier(ATTACK_BOOST_ID);
        } else {
            if (!speed.hasModifier(ATTACK_BOOST_ID)) {
                speed.addTransientModifier(ATTACK_BOOST);
            }
            if (target instanceof Player) {
                entityData.set(SCREAMING, true);
            }
        }
    }

    public static void summonFighters(Level level, LivingEntity target, int maxCount) {
        if (level.isClientSide || target == null || target instanceof EntityEndermanFighter) {
            return;
        }
        AABB box = target.getBoundingBox().inflate(16.0, 16.0, 16.0);
        int remaining = maxCount;
        for (EntityEndermanFighter fighter : level.getEntitiesOfClass(EntityEndermanFighter.class, box,
                entity -> !entity.hasCustomName() && entity.assignedTarget == null)) {
            if (remaining <= 0) {
                break;
            }
            fighter.setAssignedTarget(target);
            remaining--;
        }
        for (int count = 0; count < remaining; count++) {
            EntityEndermanFighter fighter = ModEntities.ENDERMAN_FIGHTER.get().create(level);
            if (fighter == null) {
                continue;
            }
            boolean placed = false;
            for (int i = 0; i < 16; i++) {
                double x = target.getX() - 5.0 + level.random.nextFloat() * 10.0;
                double y = target.getY() - 2.0 + level.random.nextFloat() * 4.0;
                double z = target.getZ() - 5.0 + level.random.nextFloat() * 10.0;
                BlockPos below = BlockPos.containing(x, (int) (y - 1), z);
                BlockState state = level.getBlockState(below);
                fighter.moveTo(x, (int) y, z, level.random.nextFloat() * 360.0F, 0.0F);
                if (level.noCollision(fighter) && !level.containsAnyLiquid(fighter.getBoundingBox())
                        && state.isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
                    fighter.setAssignedTarget(target);
                    level.addFreshEntity(fighter);
                    if (level instanceof ServerLevel server) {
                        for (int p = 0; p < 16; p++) {
                            server.sendParticles(ParticleTypes.PORTAL,
                                    fighter.getX(), fighter.getY() + fighter.getRandom().nextDouble() * fighter.getBbHeight(),
                                    fighter.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                        }
                    }
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                break;
            }
        }
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public void aiStep() {
        if (level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.PORTAL,
                        getX() + (random.nextDouble() - 0.5) * getBbWidth(),
                        getY() + random.nextDouble() * getBbHeight() - 0.25,
                        getZ() + (random.nextDouble() - 0.5) * getBbWidth(),
                        (random.nextDouble() - 0.5) * 2.0,
                        -random.nextDouble(),
                        (random.nextDouble() - 0.5) * 2.0);
            }
        }
        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        if (isInWaterOrRain()) {
            hurt(damageSources().drown(), 1.0F);
        }
        if ((tickCount & 0x7) == 0) {
            updateControlState();
        }
        if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
            assignedTarget = null;
            revengeTarget = null;
        }
        super.customServerAiStep();
    }

    private void updateControlState() {
        beingControlled = false;
        for (Player player : level().players()) {
            if (player.isAlive() && distanceToSqr(player) < 1024.0 && isHoldingSummon(player)) {
                beingControlled = true;
                break;
            }
        }
        entityData.set(RAGING, !beingControlled);
    }

    private static boolean isHoldingSummon(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.getItem() instanceof ItemEnderSword sword && sword.dropsMode(main) == ItemEnderSword.DROPS_SUMMON;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        entityData.set(SCREAMING, true);
        if (source.getEntity() instanceof LivingEntity living && living != this) {
            revengeTarget = living.getUUID();
        }
        if (source.getEntity() == null) {
            for (int i = 0; i < 64; i++) {
                if (teleportRandomly()) {
                    return true;
                }
            }
        }
        return super.hurt(source, amount);
    }

    private boolean teleportRandomly() {
        double x = getX() + (random.nextDouble() - 0.5) * 32.0;
        double y = getY() + (random.nextInt(8) - 4);
        double z = getZ() + (random.nextDouble() - 0.5) * 32.0;
        boolean success = randomTeleport(x, y, z, true);
        if (success) {
            level().playSound(null, xo, yo, zo, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1.0F, 1.0F);
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
        return success;
    }

    @Nullable
    private LivingEntity entityByUuid(UUID uuid) {
        if (uuid == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        var found = server.getEntity(uuid);
        return found instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Nullable
    private Player findPlayerToAttack() {
        if (beingControlled || hasCustomName()) {
            return null;
        }
        Player closest = null;
        double best = -1.0;
        for (Player player : level().players()) {
            if (!player.isAlive() || player.isSpectator() || player.getAbilities().invulnerable || isHoldingSummon(player)) {
                continue;
            }
            double dist = distanceToSqr(player);
            double sight = player.isShiftKeyDown() ? 64.0 * 0.8 : 64.0;
            if (player.isInvisible()) {
                float vis = Math.max(0.1F, player.getArmorCoverPercentage());
                sight *= 0.7 * vis;
            }
            if (dist < sight * sight && (best < 0.0 || dist < best)) {
                best = dist;
                closest = player;
            }
        }
        return closest;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isScreaming() ? SoundEvents.ENDERMAN_SCREAM : SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Raging", isRaging());
        tag.putBoolean("Screaming", isScreaming());
        if (assignedTarget != null) {
            tag.putUUID("Assigned", assignedTarget);
        }
        if (revengeTarget != null) {
            tag.putUUID("Revenge", revengeTarget);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(RAGING, tag.getBoolean("Raging"));
        entityData.set(SCREAMING, tag.getBoolean("Screaming"));
        assignedTarget = tag.hasUUID("Assigned") ? tag.getUUID("Assigned") : null;
        revengeTarget = tag.hasUUID("Revenge") ? tag.getUUID("Revenge") : null;
        if (hasCustomName()) {
            setPersistenceRequired();
        }
    }

    @Override
    public void setCustomName(@Nullable net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        if (hasCustomName()) {
            setPersistenceRequired();
        }
    }

    private abstract class AIAttackTarget extends TargetGoal {
        private final boolean requireControlled;

        AIAttackTarget(boolean requireControlled) {
            super(EntityEndermanFighter.this, false, false);
            this.requireControlled = requireControlled;
            setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (hasCustomName() || beingControlled != requireControlled) {
                return false;
            }
            LivingEntity target = resolve();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && super.canContinueToUse();
        }

        @Override
        public void start() {
            LivingEntity target = resolve();
            setTarget(target);
            super.start();
        }

        @Nullable
        protected abstract LivingEntity resolve();
    }

    private class AIAttackAssigned extends AIAttackTarget {
        AIAttackAssigned(boolean requireControlled) {
            super(requireControlled);
        }

        @Override
        protected LivingEntity resolve() {
            return entityByUuid(assignedTarget);
        }
    }

    private class AIAttackRevenge extends AIAttackTarget {
        AIAttackRevenge(boolean requireControlled) {
            super(requireControlled);
        }

        @Override
        public void start() {
            super.start();
            entityData.set(RAGING, true);
        }

        @Override
        public void stop() {
            super.stop();
            entityData.set(RAGING, beingControlled ? false : isRaging());
        }

        @Override
        protected LivingEntity resolve() {
            return entityByUuid(revengeTarget);
        }
    }

    private class AIAttackClosestPlayer extends AIAttackRevenge {
        AIAttackClosestPlayer(boolean requireControlled) {
            super(requireControlled);
        }

        @Override
        protected LivingEntity resolve() {
            return findPlayerToAttack();
        }
    }

    private class AIDespawn extends Goal {
        private final int delay;
        private int timer;

        AIDespawn(int delay) {
            this.delay = delay;
        }

        @Override
        public boolean canUse() {
            return !hasCustomName() && getTarget() == null;
        }

        @Override
        public void tick() {
            if (++timer >= delay && random.nextFloat() < 0.03F) {
                if (level() instanceof ServerLevel server) {
                    for (int i = 0; i < 16; i++) {
                        server.sendParticles(ParticleTypes.PORTAL,
                                getX() + (random.nextDouble() - 0.5) * getBbWidth() * 2.0,
                                getY() + random.nextDouble() * getBbHeight(),
                                getZ() + (random.nextDouble() - 0.5) * getBbWidth() * 2.0,
                                1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 0.7F, 1.0F);
                discard();
            }
        }

        @Override
        public void stop() {
            timer = 0;
        }
    }
}
