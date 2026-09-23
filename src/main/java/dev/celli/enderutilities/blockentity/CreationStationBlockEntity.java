package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.block.HorizontalMachineBlock;
import dev.celli.enderutilities.data.ItemInventoryData;
import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.item.ItemMemoryCard;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.menu.CreationStationMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import dev.celli.enderutilities.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class CreationStationBlockEntity extends InventoryBlockEntity implements GuiActionHandler,
        HorizontalMachineBlock.ServerTickingBlockEntity {
    public static final int MODULE_SLOTS = 4;
    public static final int STORAGE_SLOTS = 27;
    public static final int CRAFT_SLOTS = 9;
    public static final int FURNACE_SLOTS = 6;
    public static final int RESULT_SLOTS = 2;
    public static final int TOTAL = MODULE_SLOTS + STORAGE_SLOTS + CRAFT_SLOTS * 2 + FURNACE_SLOTS + RESULT_SLOTS;

    public static final int SLOT_MODULES = 0;
    public static final int SLOT_STORAGE = MODULE_SLOTS;
    public static final int SLOT_CRAFT_LEFT = SLOT_STORAGE + STORAGE_SLOTS;
    public static final int SLOT_CRAFT_RIGHT = SLOT_CRAFT_LEFT + CRAFT_SLOTS;
    public static final int SLOT_CRAFT = SLOT_CRAFT_LEFT;
    public static final int SLOT_FURNACE = SLOT_CRAFT_RIGHT + CRAFT_SLOTS;
    public static final int SLOT_RESULT_LEFT = SLOT_FURNACE + FURNACE_SLOTS;
    public static final int SLOT_RESULT_RIGHT = SLOT_RESULT_LEFT + 1;

    private int cookTimeLeft;
    private int burnTimeLeft;
    private int burnTimeFreshLeft;
    private int cookTimeRight;
    private int burnTimeRight;
    private int burnTimeFreshRight;
    private int modeMask;
    private int selectedModule;
    private int actionMode;
    public int lastInteractedCraftingGrid;
    private boolean refreshingResults;
    private final ItemStack[][] recipes = new ItemStack[10][9];

    public CreationStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATION_STATION.get(), pos, state, new LimitedItemStackHandler(TOTAL, 64)
                .validator((slot, stack) -> {
                    if (slot == SLOT_RESULT_LEFT || slot == SLOT_RESULT_RIGHT) {
                        return false;
                    }
                    if (slot >= SLOT_MODULES && slot < SLOT_STORAGE) {
                        return stack.getItem() instanceof ItemMemoryCard card
                                && card.getModuleType(stack) == ModuleType.MEMORY_CARD_ITEMS;
                    }
                    return true;
                }));
        for (int i = 0; i < recipes.length; i++) {
            java.util.Arrays.fill(recipes[i], ItemStack.EMPTY);
        }
        inventory.onChanged(() -> {
            setChanged();
            if (!refreshingResults) {
                refreshCraftResults();
            }
        });
    }

    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> modeMask;
                case 1 -> (actionMode << 2) | (selectedModule & 0x3);
                case 2 -> (getBurnProgress(1) << 8) | getBurnProgress(0);
                case 3 -> (getSmeltProgress(1) << 8) | getSmeltProgress(0);
                case 4 -> lastInteractedCraftingGrid;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> modeMask = value;
                case 1 -> {
                    selectedModule = value & 0x3;
                    actionMode = (value >> 2) & 0x7;
                }
                case 4 -> lastInteractedCraftingGrid = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public net.minecraft.world.inventory.ContainerData getData() {
        return data;
    }

    public int getModeMask() {
        return modeMask;
    }

    public int getSelectedModuleSlot() {
        return selectedModule;
    }

    public int getQuickMode() {
        return actionMode;
    }

    private int getBurnProgress(int furnace) {
        int fresh = furnace == 0 ? burnTimeFreshLeft : burnTimeFreshRight;
        if (fresh <= 0) {
            return 0;
        }
        int remaining = furnace == 0 ? burnTimeLeft : burnTimeRight;
        return remaining * 100 / fresh;
    }

    private int getSmeltProgress(int furnace) {
        int cook = furnace == 0 ? cookTimeLeft : cookTimeRight;
        int complete = (modeMask & (furnace == 0 ? 0x0040 : 0x0080)) != 0 ? 40 : 100;
        if (complete <= 0) {
            return 0;
        }
        return cook * 100 / complete;
    }

    public int resultSlot(int grid) {
        return grid == 1 ? SLOT_RESULT_RIGHT : SLOT_RESULT_LEFT;
    }

    public int craftStart(int grid) {
        return grid == 1 ? SLOT_CRAFT_RIGHT : SLOT_CRAFT_LEFT;
    }

    public void refreshCraftResults() {
        if (level == null) {
            return;
        }
        refreshingResults = true;
        inventory.setStackInSlot(SLOT_RESULT_LEFT, matchCraft(0));
        inventory.setStackInSlot(SLOT_RESULT_RIGHT, matchCraft(1));
        refreshingResults = false;
    }

    private ItemStack matchCraft(int grid) {
        if (level == null) {
            return ItemStack.EMPTY;
        }
        net.minecraft.world.item.crafting.CraftingInput input = craftInput(grid);
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(RecipeHolder::value)
                .map(recipe -> recipe.assemble(input, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private net.minecraft.world.item.crafting.CraftingInput craftInput(int grid) {
        java.util.List<ItemStack> items = new java.util.ArrayList<>(9);
        int start = craftStart(grid);
        for (int i = 0; i < 9; i++) {
            items.add(inventory.getStackInSlot(start + i));
        }
        return net.minecraft.world.item.crafting.CraftingInput.of(3, 3, items);
    }

    public void consumeCraft(int grid) {
        if (level == null) {
            return;
        }
        net.minecraft.world.item.crafting.CraftingInput input = craftInput(grid);
        var holder = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        java.util.List<ItemStack> remaining = holder
                .map(h -> (java.util.List<ItemStack>) h.value().getRemainingItems(input))
                .orElseGet(java.util.List::of);
        int start = craftStart(grid);
        for (int i = 0; i < 9; i++) {
            ItemStack slot = inventory.getStackInSlot(start + i);
            boolean keepOne = (modeMask & (grid == 0 ? 0x0002 : 0x0010)) != 0;
            if (!slot.isEmpty() && (!keepOne || slot.getCount() > 1)) {
                slot.shrink(1);
                inventory.setStackInSlot(start + i, slot);
            }
            if (i < remaining.size() && !remaining.get(i).isEmpty()) {
                ItemStack leftover = inventory.insertItem(start + i, remaining.get(i).copy(), false);
                leftover = ItemHandlerHelper.insertItemStacked(storageHandler(), leftover, false);
                if (!leftover.isEmpty() && level != null) {
                    net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1,
                            worldPosition.getZ(), leftover);
                }
            }
        }
        refreshCraftResults();
    }

    @Override
    public IItemHandler getHopperHandler() {
        return storageHandler();
    }

    @Override
    public void serverTick() {
        tickFurnace(0);
        tickFurnace(1);
        if ((modeMask & 0x0004) != 0) {
            autoCraft(0);
        }
        if ((modeMask & 0x0020) != 0) {
            autoCraft(1);
        }
    }

    private void autoCraft(int grid) {
        ItemStack result = inventory.getStackInSlot(resultSlot(grid));
        if (result.isEmpty()) {
            return;
        }
        ItemStack leftover = ItemHandlerHelper.insertItemStacked(storageHandler(), result.copy(), false);
        if (leftover.isEmpty()) {
            consumeCraft(grid);
        }
    }

    private void tickFurnace(int furnace) {
        if (level == null || level.isClientSide) {
            return;
        }
        int inputSlot = SLOT_FURNACE + furnace * 3;
        int fuelSlot = inputSlot + 1;
        int outputSlot = inputSlot + 2;
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) {
            if (furnace == 0) {
                cookTimeLeft = 0;
            } else {
                cookTimeRight = 0;
            }
            return;
        }
        ItemStack result = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
                .map(RecipeHolder::value)
                .map(recipe -> recipe.getResultItem(level.registryAccess()))
                .orElse(ItemStack.EMPTY);
        if (result.isEmpty() || !inventory.insertItem(outputSlot, result.copy(), true).isEmpty()) {
            return;
        }
        int burnTime = furnace == 0 ? burnTimeLeft : burnTimeRight;
        if (burnTime <= 0) {
            ItemStack fuel = inventory.getStackInSlot(fuelSlot);
            int gained = EnderFurnaceBlockEntity.getItemBurnTime(fuel);
            if (gained <= 0) {
                return;
            }
            inventory.extractItem(fuelSlot, 1, false);
            burnTime = gained;
            if (furnace == 0) {
                burnTimeFreshLeft = gained;
            } else {
                burnTimeFreshRight = gained;
            }
        }
        burnTime--;
        int cookTime = (furnace == 0 ? cookTimeLeft : cookTimeRight) + 1;
        int cookComplete = (modeMask & (furnace == 0 ? 0x0040 : 0x0080)) != 0 ? 40 : 100;
        if (cookTime >= cookComplete) {
            inventory.insertItem(outputSlot, result.copy(), false);
            inventory.extractItem(inputSlot, 1, false);
            cookTime = 0;
        }
        if (furnace == 0) {
            cookTimeLeft = cookTime;
            burnTimeLeft = burnTime;
        } else {
            cookTimeRight = cookTime;
            burnTimeRight = burnTime;
        }
        setChanged();
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        switch (action) {
            case 0 -> {
                if (element >= 0 && element < MODULE_SLOTS) {
                    selectedModule = element;
                }
            }
            case 1 -> moveItems(player, element);
            case 2 -> {
                if (element >= 0 && element < 6) {
                    actionMode = element;
                }
            }
            case 3 -> {
                dumpGrid(element == 1 ? SLOT_CRAFT_RIGHT : SLOT_CRAFT_LEFT);
                lastInteractedCraftingGrid = element == 1 ? 1 : 0;
            }
            case 4 -> loadStoredRecipe(element);
            case 5 -> storeStoredRecipe(element);
            case 6 -> clearStoredRecipe(element);
            case 7 -> modeMask ^= switch (element) {
                case 0 -> 0x0001;
                case 1 -> 0x0002;
                case 2 -> 0x0004;
                case 3 -> 0x0020;
                case 4 -> 0x0010;
                case 5 -> 0x0008;
                case 6 -> 0x0040;
                case 7 -> 0x0080;
                default -> 0;
            };
            case 8 -> {
                if (element == 1) {
                    sortPlayer(player);
                } else {
                    sortStorage();
                }
            }
            default -> {
            }
        }
        refreshCraftResults();
        setChanged();
    }

    private void loadStoredRecipe(int recipeId) {
        if (recipeId < 0 || recipeId >= recipes.length) {
            return;
        }
        boolean right = recipeId >= 5;
        int start = right ? SLOT_CRAFT_RIGHT : SLOT_CRAFT_LEFT;
        int gridId = right ? 1 : 0;
        setRecipeId(gridId, recipeId % 5);
        modeMask |= right ? 0x8000 : 0x4000;
        lastInteractedCraftingGrid = gridId;
        dumpGrid(start);
        ItemStack[] recipe = recipes[recipeId];
        for (int i = 0; i < CRAFT_SLOTS; i++) {
            if (recipe[i] != null && !recipe[i].isEmpty()) {
                inventory.setStackInSlot(start + i, extractMatching(recipe[i], (modeMask & (right ? 0x0008 : 0x0001)) != 0));
            }
        }
    }

    private void storeStoredRecipe(int recipeId) {
        if (recipeId < 0 || recipeId >= recipes.length) {
            return;
        }
        boolean right = recipeId >= 5;
        int start = right ? SLOT_CRAFT_RIGHT : SLOT_CRAFT_LEFT;
        int gridId = right ? 1 : 0;
        setRecipeId(gridId, recipeId % 5);
        modeMask |= right ? 0x8000 : 0x4000;
        lastInteractedCraftingGrid = gridId;
        for (int i = 0; i < CRAFT_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(start + i);
            recipes[recipeId][i] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        }
    }

    private void clearStoredRecipe(int recipeId) {
        if (recipeId < 0 || recipeId >= recipes.length) {
            return;
        }
        java.util.Arrays.fill(recipes[recipeId], ItemStack.EMPTY);
    }

    private void setRecipeId(int invId, int recipeId) {
        int shift = invId == 1 ? 11 : 8;
        int mask = invId == 1 ? 0x3800 : 0x0700;
        modeMask = (modeMask & ~mask) | ((recipeId & 0x7) << shift);
    }

    private void sortPlayer(Player player) {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        stacks.sort((a, b) -> Integer.compare(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(a.getItem()),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(b.getItem())));
        int slot = 9;
        for (ItemStack stack : stacks) {
            while (slot < 36 && !inv.getItem(slot).isEmpty()) {
                slot++;
            }
            if (slot < 36) {
                inv.setItem(slot, stack);
                slot++;
            }
        }
    }

    private void sortStorage() {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(SLOT_STORAGE + i);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
                inventory.setStackInSlot(SLOT_STORAGE + i, ItemStack.EMPTY);
            }
        }
        stacks.sort((a, b) -> Integer.compare(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(a.getItem()),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(b.getItem())));
        for (ItemStack stack : stacks) {
            ItemHandlerHelper.insertItemStacked(storageHandler(), stack, false);
        }
    }

    private void moveItems(Player player, int element) {
        var playerInv = new net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper(player.getInventory());
        switch (element) {
            case 0 -> moveRange(playerInv, storageHandler(), 9, 36, false);
            case 1 -> moveRange(playerInv, storageHandler(), 9, 36, true);
            case 2 -> fillPlayerFromStorage(playerInv, true);
            case 3 -> fillPlayerFromStorage(playerInv, false);
            case 4 -> moveRange(storageHandler(), playerInv, 0, storageHandler().getSlots(), true);
            case 5 -> moveRange(storageHandler(), playerInv, 0, storageHandler().getSlots(), false);
            default -> {
            }
        }
    }

    private static void moveRange(IItemHandler from, IItemHandler to, int start, int end, boolean matchingOnly) {
        for (int i = start; i < end && i < from.getSlots(); i++) {
            ItemStack stack = from.extractItem(i, 64, true);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack leftover = matchingOnly
                    ? ItemHandlerHelper.insertItemStacked(to, stack, true)
                    : ItemHandlerHelper.insertItemStacked(to, stack, true);
            if (matchingOnly && !hasMatching(to, stack)) {
                continue;
            }
            leftover = ItemHandlerHelper.insertItemStacked(to, from.extractItem(i, 64, false), false);
            if (!leftover.isEmpty()) {
                from.insertItem(i, leftover, false);
            }
        }
    }

    private static boolean hasMatching(IItemHandler handler, ItemStack stack) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(i), stack)) {
                return true;
            }
        }
        return false;
    }

    private void fillPlayerFromStorage(IItemHandler playerInv, boolean leaveOneFull) {
        for (int i = 0; i < playerInv.getSlots(); i++) {
            ItemStack current = playerInv.getStackInSlot(i);
            if (current.isEmpty()) {
                continue;
            }
            int need = current.getMaxStackSize() - current.getCount();
            if (need <= 0) {
                continue;
            }
            for (int s = 0; s < STORAGE_SLOTS && need > 0; s++) {
                ItemStack stored = inventory.getStackInSlot(SLOT_STORAGE + s);
                if (!ItemStack.isSameItemSameComponents(stored, current)) {
                    continue;
                }
                if (leaveOneFull && stored.getCount() <= 1) {
                    continue;
                }
                int move = Math.min(need, leaveOneFull ? stored.getCount() - 1 : stored.getCount());
                if (move <= 0) {
                    continue;
                }
                ItemStack taken = inventory.extractItem(SLOT_STORAGE + s, move, false);
                ItemStack leftover = playerInv.insertItem(i, taken, false);
                if (!leftover.isEmpty()) {
                    inventory.insertItem(SLOT_STORAGE + s, leftover, false);
                    break;
                }
                need -= taken.getCount() - leftover.getCount();
            }
        }
    }

    private void dumpGrid(int start) {
        for (int i = 0; i < CRAFT_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(start + i);
            if (!stack.isEmpty()) {
                ItemStack leftover = ItemHandlerHelper.insertItemStacked(storageHandler(), stack, false);
                inventory.setStackInSlot(start + i, leftover);
            }
        }
    }

    private void clearGrid(int start) {
        dumpGrid(start);
        for (int i = 0; i < CRAFT_SLOTS; i++) {
            ItemStack leftover = inventory.getStackInSlot(start + i);
            if (!leftover.isEmpty() && level != null) {
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1,
                        worldPosition.getZ(), leftover);
                inventory.setStackInSlot(start + i, ItemStack.EMPTY);
            }
        }
    }

    private ItemStack selectedCard() {
        for (int i = 0; i < MODULE_SLOTS; i++) {
            ItemStack card = inventory.getStackInSlot(SLOT_MODULES + i);
            if (!card.isEmpty()) {
                return card;
            }
        }
        return ItemStack.EMPTY;
    }

    private void storeRecipe(boolean right) {
        ItemStack card = selectedCard();
        if (card.isEmpty()) {
            return;
        }
        int start = right ? SLOT_CRAFT_RIGHT : SLOT_CRAFT_LEFT;
        int offset = right ? CRAFT_SLOTS : 0;
        ItemInventoryData data = card.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
        for (int i = 0; i < CRAFT_SLOTS; i++) {
            data = data.withSlot(offset + i, inventory.getStackInSlot(start + i).copyWithCount(1));
        }
        card.set(ModDataComponents.ITEM_INVENTORY, data);
    }

    private void loadRecipe(boolean right) {
        ItemStack card = selectedCard();
        if (card.isEmpty()) {
            return;
        }
        int start = right ? SLOT_CRAFT_RIGHT : SLOT_CRAFT_LEFT;
        int offset = right ? CRAFT_SLOTS : 0;
        ItemInventoryData data = card.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
        dumpGrid(start);
        for (int i = 0; i < CRAFT_SLOTS; i++) {
            ItemStack template = data.stackInSlot(offset + i);
            if (template.isEmpty()) {
                continue;
            }
            ItemStack found = extractMatching(template, (modeMask & (right ? 0x0008 : 0x0001)) != 0);
            inventory.setStackInSlot(start + i, found);
        }
    }

    private ItemStack extractMatching(ItemStack template, boolean oredict) {
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack existing = inventory.getStackInSlot(SLOT_STORAGE + i);
            if (matchesTemplate(existing, template, oredict)) {
                return inventory.extractItem(SLOT_STORAGE + i, 1, false);
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean matchesTemplate(ItemStack existing, ItemStack template, boolean oredict) {
        if (existing.isEmpty()) {
            return false;
        }
        if (ItemStack.isSameItemSameComponents(existing, template)) {
            return true;
        }
        return oredict && existing.getTags().anyMatch(template::is);
    }

    private IItemHandler storageHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return STORAGE_SLOTS;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return inventory.getStackInSlot(SLOT_STORAGE + slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return inventory.insertItem(SLOT_STORAGE + slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return inventory.extractItem(SLOT_STORAGE + slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return true;
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CookTime", cookTimeLeft);
        tag.putInt("BurnTime", burnTimeLeft);
        tag.putInt("CookTime2", cookTimeRight);
        tag.putInt("BurnTime2", burnTimeRight);
        tag.putInt("ModeMask", modeMask);
        tag.putByte("SelModule", (byte) selectedModule);
        tag.putByte("QuickMode", (byte) actionMode);
        tag.putByte("LastGrid", (byte) lastInteractedCraftingGrid);
        tag.putInt("BurnTimeFresh", burnTimeFreshLeft);
        tag.putInt("BurnTimeFresh2", burnTimeFreshRight);
        net.minecraft.nbt.ListTag recipeList = new net.minecraft.nbt.ListTag();
        for (int r = 0; r < recipes.length; r++) {
            CompoundTag recipeTag = new CompoundTag();
            recipeTag.putByte("Id", (byte) r);
            for (int i = 0; i < CRAFT_SLOTS; i++) {
                if (recipes[r][i] != null && !recipes[r][i].isEmpty()) {
                    recipeTag.put("s" + i, recipes[r][i].save(registries));
                }
            }
            recipeList.add(recipeTag);
        }
        tag.put("Recipes", recipeList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cookTimeLeft = tag.getInt("CookTime");
        burnTimeLeft = tag.getInt("BurnTime");
        cookTimeRight = tag.getInt("CookTime2");
        burnTimeRight = tag.getInt("BurnTime2");
        modeMask = tag.getInt("ModeMask");
        selectedModule = tag.getByte("SelModule");
        actionMode = tag.getByte("QuickMode");
        lastInteractedCraftingGrid = tag.getByte("LastGrid");
        burnTimeFreshLeft = tag.getInt("BurnTimeFresh");
        burnTimeFreshRight = tag.getInt("BurnTimeFresh2");
        if (tag.contains("Recipes")) {
            net.minecraft.nbt.ListTag recipeList = tag.getList("Recipes", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int r = 0; r < recipeList.size(); r++) {
                CompoundTag recipeTag = recipeList.getCompound(r);
                int id = recipeTag.getByte("Id");
                if (id < 0 || id >= recipes.length) {
                    continue;
                }
                for (int i = 0; i < CRAFT_SLOTS; i++) {
                    if (recipeTag.contains("s" + i)) {
                        recipes[id][i] = ItemStack.parseOptional(registries, recipeTag.getCompound("s" + i));
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CreationStationMenu(id, playerInventory, this);
    }
}
