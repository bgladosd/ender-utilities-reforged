package dev.celli.enderutilities.item.module;

public enum ModuleType {
    ENDERCORE(0, "endercore"),
    ENDERCAPACITOR(1, "endercapacitor"),
    LINKCRYSTAL(2, "linkcrystal"),
    MOBPERSISTENCE(3, "mobpersistence"),
    MEMORY_CARD_MISC(4, "memorycard_misc"),
    MEMORY_CARD_ITEMS(5, "memorycard_items"),
    CREATIVE_BREAKING(6, "creative_breaking"),
    INVALID(-1, "invalid");

    private final int index;
    private final String id;

    ModuleType(int index, String id) {
        this.index = index;
        this.id = id;
    }

    public int index() {
        return index;
    }

    public String id() {
        return id;
    }
}
