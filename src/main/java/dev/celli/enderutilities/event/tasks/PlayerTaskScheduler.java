package dev.celli.enderutilities.event.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class PlayerTaskScheduler {
    private static final PlayerTaskScheduler INSTANCE = new PlayerTaskScheduler();
    private final Map<UUID, List<IPlayerTask>> tasks = new HashMap<>();

    private PlayerTaskScheduler() {
    }

    public static PlayerTaskScheduler getInstance() {
        return INSTANCE;
    }

    public void addTask(Player player, IPlayerTask task) {
        task.init();
        tasks.computeIfAbsent(player.getUUID(), id -> new ArrayList<>()).add(task);
    }

    public boolean hasTask(Player player, Class<? extends IPlayerTask> type) {
        List<IPlayerTask> playerTasks = tasks.get(player.getUUID());
        if (playerTasks == null) {
            return false;
        }
        for (IPlayerTask task : playerTasks) {
            if (type.isInstance(task)) {
                return true;
            }
        }
        return false;
    }

    public void removeTask(Player player, Class<? extends IPlayerTask> type) {
        List<IPlayerTask> playerTasks = tasks.get(player.getUUID());
        if (playerTasks == null) {
            return;
        }
        Iterator<IPlayerTask> it = playerTasks.iterator();
        while (it.hasNext()) {
            IPlayerTask task = it.next();
            if (type.isInstance(task)) {
                task.stop();
                it.remove();
            }
        }
        if (playerTasks.isEmpty()) {
            tasks.remove(player.getUUID());
        }
    }

    public void runTasks(Level level, Player player) {
        List<IPlayerTask> playerTasks = tasks.get(player.getUUID());
        if (playerTasks == null || playerTasks.isEmpty()) {
            return;
        }
        Iterator<IPlayerTask> it = playerTasks.iterator();
        while (it.hasNext()) {
            IPlayerTask task = it.next();
            boolean finished = !task.canExecute(level, player) || task.execute(level, player);
            if (finished) {
                task.stop();
                it.remove();
            }
        }
        if (playerTasks.isEmpty()) {
            tasks.remove(player.getUUID());
        }
    }
}
