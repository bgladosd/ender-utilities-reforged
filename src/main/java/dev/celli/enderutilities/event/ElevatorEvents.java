package dev.celli.enderutilities.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.block.ElevatorBlock;
import dev.celli.enderutilities.blockentity.ElevatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = EnderUtilities.MOD_ID)
public final class ElevatorEvents {
    private static final Map<UUID, Boolean> WAS_SNEAKING = new ConcurrentHashMap<>();

    private ElevatorEvents() {
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide) {
            tryActivate(entity, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        boolean sneaking = player.isShiftKeyDown();
        Boolean previous = WAS_SNEAKING.put(player.getUUID(), sneaking);
        if (sneaking && !Boolean.TRUE.equals(previous)) {
            tryActivate(player, false);
        }
    }

    public static boolean tryActivate(Entity entity, boolean goingUp) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        ElevatorBlockEntity elevator;
        if (level.getBlockEntity(pos) instanceof ElevatorBlockEntity atFeet) {
            elevator = atFeet;
        } else if (level.getBlockEntity(pos.below()) instanceof ElevatorBlockEntity below) {
            elevator = below;
        } else {
            return false;
        }
        if (!(elevator.getBlockState().getBlock() instanceof ElevatorBlock)) {
            return false;
        }
        elevator.activateForEntity(entity, goingUp);
        return true;
    }
}
