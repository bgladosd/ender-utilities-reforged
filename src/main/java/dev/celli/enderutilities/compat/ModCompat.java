package dev.celli.enderutilities.compat;

import net.neoforged.fml.ModList;

public final class ModCompat {
    public static final String CURIOS_ID = "curios";

    private ModCompat() {
    }

    public static boolean curiosLoaded() {
        return ModList.get().isLoaded(CURIOS_ID);
    }
}
