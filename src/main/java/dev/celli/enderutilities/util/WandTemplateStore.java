package dev.celli.enderutilities.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.DataResult;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.data.WandTemplate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

public final class WandTemplateStore {
    private WandTemplateStore() {
    }

    public static Path directory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(EnderUtilities.MOD_ID).resolve("builderswand");
    }

    public static String fileName(Player player, String name, int slot) {
        String safe = name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        if (safe.isEmpty()) {
            safe = "default";
        }
        return player.getUUID().toString().replace("-", "") + "_" + Math.max(0, slot) + "_" + safe + ".nbt";
    }

    public static void write(MinecraftServer server, Player player, WandTemplate template, int slot) {
        if (server == null || player == null || template.isEmpty()) {
            return;
        }
        Path dir = directory(server);
        try {
            Files.createDirectories(dir);
            DataResult<net.minecraft.nbt.Tag> encoded = WandTemplate.CODEC.encodeStart(
                    server.registryAccess().createSerializationContext(NbtOps.INSTANCE), template);
            Optional<net.minecraft.nbt.Tag> tag = encoded.resultOrPartial(EnderUtilities.LOGGER::warn);
            if (tag.isPresent() && tag.get() instanceof CompoundTag compound) {
                NbtIo.writeCompressed(compound, dir.resolve(fileName(player, template.name(), slot)));
            }
        } catch (IOException e) {
            EnderUtilities.LOGGER.warn("Failed to save builders wand template", e);
        }
    }

    public static Optional<WandTemplate> read(MinecraftServer server, Player player, String name, int slot) {
        if (server == null || player == null) {
            return Optional.empty();
        }
        Path file = directory(server).resolve(fileName(player, name, slot));
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            if (LegacyNbtMigrator.isLegacyTemplate(tag)) {
                Optional<WandTemplate> migrated = LegacyNbtMigrator.migrateTemplate(tag, server.registryAccess());
                if (migrated.isEmpty()) {
                    EnderUtilities.LOGGER.warn("Skipping unmapped 1.12 builders wand template {}", file.getFileName());
                    return migrated;
                }
                write(server, player, migrated.get(), slot);
                return migrated;
            }
            Optional<WandTemplate> modern = WandTemplate.CODEC.parse(
                    server.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag)
                    .result();
            if (modern.isPresent()) {
                return modern;
            }
            Optional<WandTemplate> fallback = LegacyNbtMigrator.migrateTemplate(tag, server.registryAccess());
            if (fallback.isEmpty()) {
                EnderUtilities.LOGGER.warn("Skipping unmapped 1.12 builders wand template {}", file.getFileName());
            }
            fallback.ifPresent(template -> write(server, player, template, slot));
            return fallback;
        } catch (Exception e) {
            EnderUtilities.LOGGER.warn("Failed to load builders wand template {}", file.getFileName(), e);
            return Optional.empty();
        }
    }
}
