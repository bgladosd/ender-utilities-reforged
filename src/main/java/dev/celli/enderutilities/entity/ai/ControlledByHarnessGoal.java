package dev.celli.enderutilities.entity.ai;

import java.util.EnumSet;

import dev.celli.enderutilities.item.ItemMobHarness;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ControlledByHarnessGoal extends Goal {
    private final Mob mob;

    public ControlledByHarnessGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    public static boolean riderHasHarness(LivingEntity rider) {
        if (!(rider instanceof Player player)) {
            return false;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ItemMobHarness) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canUse() {
        return mob.isVehicle() && riderHasHarness(mob.getControllingPassenger());
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (!(mob.getControllingPassenger() instanceof Player player)) {
            return;
        }
        mob.setYRot(player.getYRot());
        mob.yRotO = mob.getYRot();
        mob.setXRot(player.getXRot() * 0.5F);
        mob.setYBodyRot(mob.getYRot());
        mob.setYHeadRot(mob.getYRot());
        float strafe = player.xxa * 0.5F;
        float forward = player.zza;
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }
        double speed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (speed <= 0.0) {
            speed = 0.25;
        }
        mob.setSpeed((float) speed);
        mob.travel(new Vec3(strafe, 0.0, forward));
    }
}
