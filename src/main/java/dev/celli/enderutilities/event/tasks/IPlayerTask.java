package dev.celli.enderutilities.event.tasks;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IPlayerTask {
    default void init() {
    }

    boolean canExecute(Level level, Player player);

    boolean execute(Level level, Player player);

    default void stop() {
    }
}
