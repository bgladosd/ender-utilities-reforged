package dev.celli.enderutilities.reference;

public final class HotKeys {
    public static final int BASE_KEY_MASK = 0x0000FFFF;
    public static final int KEYBIND_ID_TOGGLE_MODE = 0x00000001;
    public static final int KEYCODE_SCROLL = 0x00001000;
    public static final int KEYCODE_CUSTOM_1 = 0x10000000;

    public static final int MOD_NONE = 0x00000000;
    public static final int MOD_SHIFT = 0x00010000;
    public static final int MOD_CTRL = 0x00020000;
    public static final int MOD_ALT = 0x00040000;

    public static final int SCROLL_MODIFIER_REVERSE = 0x00100000;

    public static final int MOD_SHIFT_CTRL = MOD_SHIFT | MOD_CTRL;
    public static final int MOD_SHIFT_ALT = MOD_SHIFT | MOD_ALT;
    public static final int MOD_CTRL_ALT = MOD_CTRL | MOD_ALT;
    public static final int MOD_SHIFT_CTRL_ALT = MOD_SHIFT | MOD_CTRL | MOD_ALT;

    private HotKeys() {
    }

    public enum EnumKey {
        TOGGLE(KEYBIND_ID_TOGGLE_MODE),
        SCROLL(KEYCODE_SCROLL);

        private final int keyCode;

        EnumKey(int keyCode) {
            this.keyCode = keyCode;
        }

        public boolean matches(int key, int modifiers) {
            return (key & BASE_KEY_MASK) == keyCode && (key & 0x000F0000) == modifiers;
        }

        public boolean matches(int key, int modifiers, int extra) {
            int actual = key & 0x000F0000;
            return (key & BASE_KEY_MASK) == keyCode && (actual == modifiers || actual == (modifiers | extra));
        }

        public static boolean keypressContainsShift(int key) {
            return (key & MOD_SHIFT) != 0;
        }

        public static boolean keypressContainsControl(int key) {
            return (key & MOD_CTRL) != 0;
        }

        public static boolean keypressContainsAlt(int key) {
            return (key & MOD_ALT) != 0;
        }

        public static boolean keypressActionIsReversed(int key) {
            return (key & SCROLL_MODIFIER_REVERSE) != 0;
        }
    }
}
