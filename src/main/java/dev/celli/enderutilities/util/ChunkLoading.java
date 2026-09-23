package dev.celli.enderutilities.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 1.21 analog of 1.12 {@code ForgeChunkManager} tickets.
 * Vanilla {@link ServerLevel#setChunkForced} is the current force-load API
 * ({@code TicketHelper} / ticket controllers are for NeoForge-owned persistent
 * tickets; EU only needs timed loads). Tickets are also stored in {@link SavedData}
 * so they are re-applied with {@code setChunkForced} after a restart or dimension load.
 * {@code owner != null} / {@code playerTicket} is the orig player vs mod ticket split.
 */
public final class ChunkLoading {
    public static final int TIMEOUT_TELEPORT = 10;
    public static final int TIMEOUT_BAG = 15;
    public static final int TIMEOUT_BUCKET = 30;
    public static final int TIMEOUT_TOOLS = 30;
    public static final int TIMEOUT_TOOL_INVENTORY = 15;

    private static final ChunkLoading INSTANCE = new ChunkLoading();
    private final List<Ticket> tickets = new ArrayList<>();
    private boolean restored;

    public record Ticket(
            ResourceKey<Level> dimension,
            ChunkPos pos,
            long expireTick,
            int timeoutSeconds,
            UUID owner,
            boolean playerTicket
    ) {
        boolean sameChunk(ResourceKey<Level> dimension, ChunkPos pos) {
            return this.dimension.equals(dimension) && this.pos.equals(pos);
        }
    }

    private ChunkLoading() {
    }

    public static ChunkLoading getInstance() {
        return INSTANCE;
    }

    public void reset() {
        tickets.clear();
        restored = false;
    }

    public boolean forceChunk(ServerLevel level, BlockPos pos, int timeoutSeconds) {
        return forceChunk(level, pos.getX() >> 4, pos.getZ() >> 4, timeoutSeconds, null);
    }

    public boolean forceChunk(ServerLevel level, BlockPos pos, int timeoutSeconds, UUID owner) {
        return forceChunk(level, pos.getX() >> 4, pos.getZ() >> 4, timeoutSeconds, owner);
    }

    public boolean forceChunk(ServerLevel level, int chunkX, int chunkZ, int timeoutSeconds) {
        return forceChunk(level, chunkX, chunkZ, timeoutSeconds, null);
    }

    public boolean forceChunk(ServerLevel level, int chunkX, int chunkZ, int timeoutSeconds, UUID owner) {
        if (level == null) {
            return false;
        }
        ensureRestored(level.getServer());
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        int duration = Math.max(1, timeoutSeconds);
        long expire = level.getGameTime() + duration * 20L;
        boolean playerTicket = owner != null;
        for (int i = 0; i < tickets.size(); i++) {
            Ticket existing = tickets.get(i);
            if (existing.sameChunk(level.dimension(), chunk) && existing.playerTicket() == playerTicket
                    && (owner == null || owner.equals(existing.owner()))) {
                int nextDuration = Math.max(duration, existing.timeoutSeconds());
                if (expire > existing.expireTick() || nextDuration > existing.timeoutSeconds()) {
                    tickets.set(i, new Ticket(level.dimension(), chunk, Math.max(expire, existing.expireTick()),
                            nextDuration, existing.owner(), existing.playerTicket()));
                    persist(level.getServer());
                }
                level.setChunkForced(chunk.x, chunk.z, true);
                level.getChunk(chunk.x, chunk.z);
                return true;
            }
        }
        level.setChunkForced(chunk.x, chunk.z, true);
        level.getChunk(chunk.x, chunk.z);
        tickets.add(new Ticket(level.dimension(), chunk, expire, duration, owner, playerTicket));
        persist(level.getServer());
        return true;
    }

    public boolean refresh(ServerLevel level, BlockPos pos) {
        return refresh(level, pos.getX() >> 4, pos.getZ() >> 4, -1);
    }

    public boolean refresh(ServerLevel level, BlockPos pos, int timeoutSeconds) {
        return refresh(level, pos.getX() >> 4, pos.getZ() >> 4, timeoutSeconds);
    }

    public boolean refresh(ServerLevel level, int chunkX, int chunkZ, int timeoutSeconds) {
        if (level == null) {
            return false;
        }
        return refresh(level.getServer(), level.dimension(), chunkX, chunkZ, timeoutSeconds, level.getGameTime());
    }

    public boolean refresh(
            MinecraftServer server,
            ResourceKey<Level> dimension,
            int chunkX,
            int chunkZ,
            int timeoutSeconds,
            long now
    ) {
        if (server == null) {
            return false;
        }
        ensureRestored(server);
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        boolean found = false;
        for (int i = 0; i < tickets.size(); i++) {
            Ticket existing = tickets.get(i);
            if (!existing.sameChunk(dimension, chunk)) {
                continue;
            }
            int fresh = existing.timeoutSeconds();
            if (timeoutSeconds >= 0 && timeoutSeconds > fresh) {
                fresh = timeoutSeconds;
            }
            long expire = now + Math.max(1, fresh) * 20L;
            tickets.set(i, new Ticket(existing.dimension(), existing.pos(), expire, fresh,
                    existing.owner(), existing.playerTicket()));
            found = true;
        }
        if (found) {
            persist(server);
            ServerLevel level = server.getLevel(dimension);
            if (level != null) {
                level.setChunkForced(chunk.x, chunk.z, true);
            }
        }
        return found;
    }

    public void restoreDimension(ServerLevel level) {
        if (level == null) {
            return;
        }
        ensureRestored(level.getServer());
        for (Ticket ticket : tickets) {
            if (ticket.dimension().equals(level.dimension())) {
                level.setChunkForced(ticket.pos().x, ticket.pos().z, true);
            }
        }
    }

    public void tick(MinecraftServer server) {
        ensureRestored(server);
        if (tickets.isEmpty()) {
            return;
        }
        Iterator<Ticket> it = tickets.iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Ticket ticket = it.next();
            ServerLevel level = server.getLevel(ticket.dimension());
            if (level == null) {
                // Do not load extra worlds; wait until this dimension is already running.
                continue;
            }
            if (level.getGameTime() >= ticket.expireTick()) {
                it.remove();
                changed = true;
                boolean stillNeeded = false;
                for (Ticket remaining : tickets) {
                    if (remaining.sameChunk(ticket.dimension(), ticket.pos())) {
                        stillNeeded = true;
                        break;
                    }
                }
                if (!stillNeeded) {
                    level.setChunkForced(ticket.pos().x, ticket.pos().z, false);
                }
            } else if (!level.getForcedChunks().contains(ChunkPos.asLong(ticket.pos().x, ticket.pos().z))) {
                // Dimension was not loaded during restore; re-apply without loading extra worlds.
                level.setChunkForced(ticket.pos().x, ticket.pos().z, true);
            }
        }
        if (changed) {
            persist(server);
        }
    }

    private void ensureRestored(MinecraftServer server) {
        if (server == null || restored) {
            return;
        }
        Store store = Store.get(server);
        tickets.clear();
        tickets.addAll(store.tickets);
        for (Ticket ticket : tickets) {
            ServerLevel level = server.getLevel(ticket.dimension());
            if (level != null) {
                level.setChunkForced(ticket.pos().x, ticket.pos().z, true);
            }
        }
        restored = true;
    }

    private void persist(MinecraftServer server) {
        if (server == null) {
            return;
        }
        Store store = Store.get(server);
        store.tickets.clear();
        store.tickets.addAll(tickets);
        store.setDirty();
    }

    private static final class Store extends SavedData {
        private static final String NAME = "enderutilities_chunkloading";
        private final List<Ticket> tickets = new ArrayList<>();

        static Store get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(Store::new, Store::load, DataFixTypes.LEVEL), NAME);
        }

        static Store load(CompoundTag nbt, HolderLookup.Provider registries) {
            Store store = new Store();
            ListTag list = nbt.getList("Tickets", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                ResourceLocation dimId = ResourceLocation.tryParse(tag.getString("Dim"));
                if (dimId == null) {
                    continue;
                }
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimId);
                UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
                boolean playerTicket = tag.contains("Player") ? tag.getBoolean("Player") : owner != null;
                int timeout = tag.contains("Timeout") ? Math.max(1, tag.getInt("Timeout")) : TIMEOUT_TELEPORT;
                store.tickets.add(new Ticket(dim, new ChunkPos(tag.getInt("X"), tag.getInt("Z")),
                        tag.getLong("Expire"), timeout, owner, playerTicket));
            }
            return store;
        }

        @Override
        public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Ticket ticket : tickets) {
                CompoundTag tag = new CompoundTag();
                tag.putString("Dim", ticket.dimension().location().toString());
                tag.putInt("X", ticket.pos().x);
                tag.putInt("Z", ticket.pos().z);
                tag.putLong("Expire", ticket.expireTick());
                tag.putInt("Timeout", ticket.timeoutSeconds());
                tag.putBoolean("Player", ticket.playerTicket());
                if (ticket.owner() != null) {
                    tag.putUUID("Owner", ticket.owner());
                }
                list.add(tag);
            }
            nbt.put("Tickets", list);
            return nbt;
        }
    }
}
