package dev.celli.enderutilities.client;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.world.entity.player.Player;

public final class ClientAccess {
    public static BooleanSupplier shiftDown = () -> false;
    public static Supplier<Player> clientPlayer = () -> null;

    private ClientAccess() {
    }

    public static boolean isShiftDown() {
        return shiftDown.getAsBoolean();
    }

    public static Player player() {
        return clientPlayer.get();
    }
}
