package dev.celli.enderutilities.client;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.module.IKeyBound;
import dev.celli.enderutilities.item.module.IKeyBoundUnselected;
import dev.celli.enderutilities.network.KeybindPayload;
import dev.celli.enderutilities.reference.HotKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.item.ItemBuildersWand;
import dev.celli.enderutilities.item.ItemHandyBag;
import dev.celli.enderutilities.network.ItemGuiActionPayload;
import dev.celli.enderutilities.network.OpenHandyBagPayload;

@EventBusSubscriber(modid = EnderUtilities.MOD_ID, value = Dist.CLIENT)
public final class ClientInputHandler {
    private static long lastShiftPress;
    private static int lastShiftKey;

    private ClientInputHandler() {
    }

    @SubscribeEvent
    public static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof HandyBagScreen)) {
            return;
        }
        int key = event.getKeyCode();
        if (key != GLFW.GLFW_KEY_LEFT_SHIFT && key != GLFW.GLFW_KEY_RIGHT_SHIFT) {
            return;
        }
        long now = System.currentTimeMillis();
        if (key == lastShiftKey && now - lastShiftPress <= 500) {
            lastShiftPress = 0;
            lastShiftKey = 0;
            PacketDistributor.sendToServer(new ItemGuiActionPayload(ItemHandyBag.GUI_ACTION_TOGGLE_SHIFTCLICK_DOUBLETAP, 0));
        } else {
            lastShiftPress = now;
            lastShiftKey = key;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        while (EnderUtilitiesClient.TOGGLE_MODE.consumeClick()) {
            ItemStack held = minecraft.player.getMainHandItem();
            if (held.getItem() instanceof ItemBuildersWand
                    && ItemBuildersWand.Mode.fromStack(held) == ItemBuildersWand.Mode.COPY
                    && currentModifierMask() == 0) {
                minecraft.setScreen(new BuilderWandTemplateScreen());
                continue;
            }
            if (!isHoldingKeyBoundItem(minecraft.player)) {
                continue;
            }
            PacketDistributor.sendToServer(new KeybindPayload(HotKeys.KEYBIND_ID_TOGGLE_MODE | currentModifierMask()));
        }
        while (EnderUtilitiesClient.ACTIVATE_UNSELECTED.consumeClick()) {
            if (isHoldingKeyBoundItem(minecraft.player) || !hasKeyBoundUnselectedItem(minecraft.player)) {
                continue;
            }
            PacketDistributor.sendToServer(new KeybindPayload(HotKeys.KEYBIND_ID_TOGGLE_MODE | currentModifierMask()));
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || event.getScrollDeltaY() == 0) {
            return;
        }
        if (!isHoldingKeyBoundItem(minecraft.player)) {
            return;
        }
        int mask = currentModifierMask();
        if (mask == 0) {
            return;
        }
        int key = HotKeys.KEYCODE_SCROLL | mask;
        if (event.getScrollDeltaY() > 0) {
            key |= HotKeys.SCROLL_MODIFIER_REVERSE;
        }
        PacketDistributor.sendToServer(new KeybindPayload(key));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide) {
            return;
        }
        if (!Screen.hasControlDown()) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof ItemBuildersWand)) {
            return;
        }
        PacketDistributor.sendToServer(new KeybindPayload(HotKeys.KEYCODE_CUSTOM_1));
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof InventoryScreen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        boolean sneakMatches = minecraft.player.isShiftKeyDown() == ModCommonConfig.HANDY_BAG_OPEN_REQUIRES_SNEAK.get();
        if (!sneakMatches || ItemHandyBag.getOpenableBag(minecraft.player).isEmpty()) {
            return;
        }
        PacketDistributor.sendToServer(new OpenHandyBagPayload());
        event.setCanceled(true);
    }

    private static boolean isHoldingKeyBoundItem(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof IKeyBound) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasKeyBoundUnselectedItem(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() instanceof IKeyBoundUnselected) {
                return true;
            }
        }
        return false;
    }

    private static int currentModifierMask() {
        int mask = 0;
        if (Screen.hasShiftDown()) {
            mask |= HotKeys.MOD_SHIFT;
        }
        if (Screen.hasControlDown()) {
            mask |= HotKeys.MOD_CTRL;
        }
        if (Screen.hasAltDown()) {
            mask |= HotKeys.MOD_ALT;
        }
        return mask;
    }
}

