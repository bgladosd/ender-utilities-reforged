package dev.celli.enderutilities.event;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.block.PortalBlock;
import dev.celli.enderutilities.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = EnderUtilities.MOD_ID)
public final class PortalTickHandler {
    private static final Set<UUID> PORTAL_FLAGS = ConcurrentHashMap.newKeySet();

    private PortalTickHandler() {
    }

    public static void addPlayerToTeleport(ServerPlayer player) {
        PORTAL_FLAGS.add(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        teleportPlayers(event.getServer());
        dev.celli.enderutilities.util.ChunkLoading.getInstance().tick(event.getServer());
    }

    private static void teleportPlayers(MinecraftServer server) {
        if (PORTAL_FLAGS.isEmpty()) {
            return;
        }
        for (UUID uuid : PORTAL_FLAGS) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                continue;
            }
            BlockPos pos = player.blockPosition();
            for (int i = 0; i < 3; i++) {
                BlockState state = player.level().getBlockState(pos);
                if (state.is(ModBlocks.PORTAL.get())) {
                    AABB box = state.getShape(player.level(), pos).bounds().move(pos);
                    if (player.getBoundingBox().intersects(box)) {
                        ((PortalBlock) state.getBlock()).teleportEntity(player.level(), pos, state, player);
                        break;
                    }
                }
                pos = pos.above();
            }
        }
        PORTAL_FLAGS.clear();
    }

    @SubscribeEvent
    public static void onLevelLoad(net.neoforged.neoforge.event.level.LevelEvent.Load event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            dev.celli.enderutilities.util.ChunkLoading.getInstance().restoreDimension(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        dev.celli.enderutilities.util.ChunkLoading.getInstance().reset();
    }
}
