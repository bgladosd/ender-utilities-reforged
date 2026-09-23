package dev.celli.enderutilities.compat.curios;

import java.util.Map;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * Compile-only Curios API. Call only when {@code curios} is loaded.
 * Maps orig Baubles-style identifiers (necklace, ring, belt, head, body, charm, back)
 * onto named Curios slot types instead of "first 7 equipped slots".
 */
public final class CuriosAccess {
    public static final int BAUBLE_SLOT_COUNT = 7;
    private static final String[] IDENTIFIERS = {
            "necklace", "ring", "belt", "head", "body", "charm", "back"
    };
    private static final String[] ALIASES = {
            "amulet", null, null, null, "chest", null, null
    };

    private CuriosAccess() {
    }

    /**
     * Must be {@link IItemHandlerModifiable}: NeoForge {@code SlotItemHandler.set}
     * casts the handler when the client applies {@code container_set_content}.
     */
    public static IItemHandlerModifiable playerCurios(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(CuriosAccess::namedBaubleSlots)
                .orElseGet(() -> new EmptyHandler(BAUBLE_SLOT_COUNT));
    }

    public static boolean mayPlace(Player player, int slot, ItemStack stack) {
        return playerCurios(player).isItemValid(slot, stack);
    }

    private static IItemHandlerModifiable namedBaubleSlots(ICuriosItemHandler curios) {
        Map<String, ICurioStacksHandler> map = curios.getCurios();
        IItemHandlerModifiable[] handlers = new IItemHandlerModifiable[BAUBLE_SLOT_COUNT];
        int[] indices = new int[BAUBLE_SLOT_COUNT];
        for (int i = 0; i < IDENTIFIERS.length; i++) {
            IDynamicStackHandler found = find(map, IDENTIFIERS[i], ALIASES[i]);
            if (found != null && found.getSlots() > 0) {
                handlers[i] = found;
                indices[i] = 0;
            } else {
                handlers[i] = new EmptyHandler(1);
                indices[i] = 0;
            }
        }
        return new NamedSlotsHandler(handlers, indices);
    }

    private static IDynamicStackHandler find(Map<String, ICurioStacksHandler> map, String id, String alias) {
        ICurioStacksHandler stacks = map.get(id);
        if (stacks == null && alias != null) {
            stacks = map.get(alias);
        }
        return stacks != null ? stacks.getStacks() : null;
    }

    private static final class NamedSlotsHandler implements IItemHandlerModifiable {
        private final IItemHandlerModifiable[] handlers;
        private final int[] indices;

        private NamedSlotsHandler(IItemHandlerModifiable[] handlers, int[] indices) {
            this.handlers = handlers;
            this.indices = indices;
        }

        @Override
        public int getSlots() {
            return handlers.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inRange(slot) ? handlers[slot].getStackInSlot(indices[slot]) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return inRange(slot) ? handlers[slot].insertItem(indices[slot], stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return inRange(slot) ? handlers[slot].extractItem(indices[slot], amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inRange(slot) ? handlers[slot].getSlotLimit(indices[slot]) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inRange(slot) && handlers[slot].isItemValid(indices[slot], stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (inRange(slot)) {
                handlers[slot].setStackInSlot(indices[slot], stack);
            }
        }

        private boolean inRange(int slot) {
            return slot >= 0 && slot < handlers.length;
        }
    }

    /**
     * Fallback when Curios has no inventory on this player (common on the client
     * while the menu is first synced). Stores stacks so container packets can
     * populate the Inventory Swapper slots.
     */
    static final class EmptyHandler extends ItemStackHandler {
        EmptyHandler(int slots) {
            super(slots);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
