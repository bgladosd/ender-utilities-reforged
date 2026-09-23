package dev.celli.enderutilities.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import dev.celli.enderutilities.EnderUtilities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = EnderUtilities.MOD_ID, dist = Dist.CLIENT)
public class EnderUtilitiesClient {
    public static final KeyMapping TOGGLE_MODE = new KeyMapping(
            "enderutilities.key.togglemode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "category.enderutilities"
    );
    public static final KeyMapping ACTIVATE_UNSELECTED = new KeyMapping(
            "enderutilities.key.activate_unselected",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "category.enderutilities"
    );

    public EnderUtilitiesClient(IEventBus modEventBus, ModContainer container) {
        ClientAccess.shiftDown = net.minecraft.client.gui.screens.Screen::hasShiftDown;
        ClientAccess.clientPlayer = () -> net.minecraft.client.Minecraft.getInstance().player;
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onRegisterKeys);
        modEventBus.addListener(this::onRegisterScreens);
        modEventBus.addListener(this::onRegisterRenderers);
        modEventBus.addListener(this::onRegisterGeometryLoaders);
        modEventBus.addListener(this::onRegisterBlockColors);
        modEventBus.addListener(this::onRegisterItemColors);
        modEventBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation charged = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "charged");
            registerChargedProperty(dev.celli.enderutilities.registry.ModItems.ENDER_CAPACITOR_BASIC.get(), charged);
            registerChargedProperty(dev.celli.enderutilities.registry.ModItems.ENDER_CAPACITOR_ENHANCED.get(), charged);
            registerChargedProperty(dev.celli.enderutilities.registry.ModItems.ENDER_CAPACITOR_ADVANCED.get(), charged);
            registerChargedProperty(dev.celli.enderutilities.registry.ModItems.ENDER_CAPACITOR_CREATIVE.get(), charged);

            ResourceLocation usetime = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "usetime");
            ResourceLocation inuse = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "inuse");
            registerPorterProperties(dev.celli.enderutilities.registry.ModItems.ENDER_PORTER.get(), usetime, inuse);
            registerPorterProperties(dev.celli.enderutilities.registry.ModItems.ENDER_PORTER_ADVANCED.get(), usetime, inuse);

            ResourceLocation signed = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "signed");
            ItemProperties.register(dev.celli.enderutilities.registry.ModItems.PET_CONTRACT.get(), signed,
                    (stack, level, entity, seed) -> dev.celli.enderutilities.item.ItemPetContract.isSigned(stack) ? 1.0F : 0.0F);

            ResourceLocation linked = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "linked");
            ItemProperties.register(dev.celli.enderutilities.registry.ModItems.MOB_HARNESS.get(), linked,
                    (stack, level, entity, seed) -> dev.celli.enderutilities.item.ItemMobHarness.hasTarget(stack) ? 1.0F : 0.0F);

            ResourceLocation ender = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "ender");
            ResourceLocation locked = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "locked");
            ItemProperties.register(dev.celli.enderutilities.registry.ModItems.ENDER_BAG.get(), ender,
                    (stack, level, entity, seed) -> dev.celli.enderutilities.item.ItemEnderBag.isEnderMode(stack) ? 1.0F : 0.0F);
            ItemProperties.register(dev.celli.enderutilities.registry.ModItems.ENDER_BAG.get(), locked,
                    (stack, level, entity, seed) -> dev.celli.enderutilities.item.ItemEnderBag.isLocked(stack) ? 1.0F : 0.0F);

            ResourceLocation carrying = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "carrying");
            ItemProperties.register(dev.celli.enderutilities.registry.ModItems.DOLLY.get(), carrying,
                    (stack, level, entity, seed) -> dev.celli.enderutilities.item.ItemDolly.isCarryingBlock(stack) ? 1.0F : 0.0F);

            ResourceLocation mode = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "mode");
            ItemProperties.register(dev.celli.enderutilities.registry.ModItems.BUILDERS_WAND.get(), mode,
                    (stack, level, entity, seed) -> {
                        int m = stack.getOrDefault(dev.celli.enderutilities.registry.ModDataComponents.ITEM_MODE, 0);
                        return (m % 5) / 5.0F;
                    });
            ItemProperties.register(dev.celli.enderutilities.registry.ModItems.LIVING_MANIPULATOR.get(), mode,
                    (stack, level, entity, seed) ->
                            stack.getOrDefault(dev.celli.enderutilities.registry.ModDataComponents.ITEM_MODE, 0) / 2.0F);
            ItemProperties.register(dev.celli.enderutilities.registry.ModItems.ENDER_BUCKET.get(), mode,
                    (stack, level, entity, seed) -> {
                        if (!(stack.getItem() instanceof dev.celli.enderutilities.item.ItemEnderBucket bucket)) {
                            return 0.0F;
                        }
                        return bucket.mode(stack) / 3.0F;
                    });
            ItemProperties.register(dev.celli.enderutilities.registry.ModItems.ENDER_BUCKET.get(), linked,
                    (stack, level, entity, seed) ->
                            dev.celli.enderutilities.item.ItemEnderBucket.isLinked(stack) ? 1.0F : 0.0F);

            ResourceLocation fastmode = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "fastmode");
            ResourceLocation broken = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "broken");
            ItemProperties.register(dev.celli.enderutilities.registry.ModItems.VOID_PICKAXE.get(), fastmode,
                    (stack, level, entity, seed) ->
                            stack.getItem() instanceof dev.celli.enderutilities.item.tool.ItemVoidPickaxe pick && pick.isFastMode(stack) ? 1.0F : 0.0F);
            ItemProperties.register(dev.celli.enderutilities.registry.ModItems.VOID_PICKAXE.get(), broken,
                    (stack, level, entity, seed) ->
                            stack.getItem() instanceof dev.celli.enderutilities.item.tool.ItemVoidPickaxe pick && pick.isToolBroken(stack) ? 1.0F : 0.0F);

            ResourceLocation pull = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "pull");
            ResourceLocation pulling = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "pulling");
            var bow = dev.celli.enderutilities.registry.ModItems.ENDER_BOW.get();
            ItemProperties.register(bow, pull, (stack, level, entity, seed) -> {
                if (entity == null || entity.getUseItem() != stack) {
                    return 0.0F;
                }
                return (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
            });
            ItemProperties.register(bow, pulling, (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            ItemProperties.register(bow, broken, (stack, level, entity, seed) ->
                    stack.getItem() instanceof dev.celli.enderutilities.item.ItemEnderBow enderBow && enderBow.isBroken(stack) ? 1.0F : 0.0F);
            ItemProperties.register(bow, mode, (stack, level, entity, seed) ->
                    stack.getItem() instanceof dev.celli.enderutilities.item.ItemEnderBow enderBow
                            && enderBow.getBowMode(stack) == dev.celli.enderutilities.item.ItemEnderBow.BOW_MODE_TP_SELF ? 1.0F : 0.0F);

            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    dev.celli.enderutilities.registry.ModBlocks.ENDER_ELEVATOR.get(),
                    net.minecraft.client.renderer.RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    dev.celli.enderutilities.registry.ModBlocks.ENDER_ELEVATOR_SLAB.get(),
                    net.minecraft.client.renderer.RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    dev.celli.enderutilities.registry.ModBlocks.ENDER_ELEVATOR_LAYER.get(),
                    net.minecraft.client.renderer.RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    dev.celli.enderutilities.registry.ModBlocks.PORTAL_PANEL.get(),
                    net.minecraft.client.renderer.RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    dev.celli.enderutilities.registry.ModBlocks.PORTAL.get(),
                    net.minecraft.client.renderer.RenderType.translucent());
        });
    }

    private static void registerPorterProperties(net.minecraft.world.item.Item item, ResourceLocation usetime, ResourceLocation inuse) {
        ItemProperties.register(item, usetime, (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }
            var active = entity.getUseItem();
            return !active.isEmpty() && active.getItem() instanceof dev.celli.enderutilities.item.ItemEnderPorter
                    ? (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 60.0F
                    : 0.0F;
        });
        ItemProperties.register(item, inuse, (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    private static void registerChargedProperty(net.minecraft.world.item.Item item, ResourceLocation charged) {
        ItemProperties.register(item, charged, (stack, level, entity, seed) ->
                stack.getItem() instanceof dev.celli.enderutilities.item.ItemEnderCapacitor capacitor
                        && capacitor.getCharge(stack) > 0 ? 1.0F : 0.0F);
    }

    private void onRegisterBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 1) {
                return 0xFFFFFF;
            }
            return state.getValue(dev.celli.enderutilities.block.ElevatorBlock.COLOR).getMapColor().col;
        },
                dev.celli.enderutilities.registry.ModBlocks.ENDER_ELEVATOR.get(),
                dev.celli.enderutilities.registry.ModBlocks.ENDER_ELEVATOR_SLAB.get(),
                dev.celli.enderutilities.registry.ModBlocks.ENDER_ELEVATOR_LAYER.get());
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex >= 0 && tintIndex <= 8 && pos != null && level != null
                    && level.getBlockEntity(pos) instanceof dev.celli.enderutilities.blockentity.PortalPanelBlockEntity panel) {
                return panel.getColor(tintIndex);
            }
            return 0xFFFFFF;
        }, dev.celli.enderutilities.registry.ModBlocks.PORTAL_PANEL.get());
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 1 && pos != null && level != null
                    && level.getBlockEntity(pos) instanceof dev.celli.enderutilities.blockentity.PortalBlockEntity portal) {
                return portal.getColor();
            }
            return 0xA010F0;
        }, dev.celli.enderutilities.registry.ModBlocks.PORTAL.get());
    }

    private void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 1) {
                return 0xFFFFFF;
            }
            var color = stack.get(dev.celli.enderutilities.registry.ModDataComponents.ELEVATOR_COLOR);
            if (color != null) {
                return color.getMapColor().col;
            }
            var blockState = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            if (blockState != null) {
                BlockState state = blockState.apply(dev.celli.enderutilities.registry.ModBlocks.ENDER_ELEVATOR.get().defaultBlockState());
                if (state.hasProperty(dev.celli.enderutilities.block.ElevatorBlock.COLOR)) {
                    return state.getValue(dev.celli.enderutilities.block.ElevatorBlock.COLOR).getMapColor().col;
                }
            }
            return DyeColor.WHITE.getMapColor().col;
        },
                dev.celli.enderutilities.registry.ModItems.ENDER_ELEVATOR.get(),
                dev.celli.enderutilities.registry.ModItems.ENDER_ELEVATOR_SLAB.get(),
                dev.celli.enderutilities.registry.ModItems.ENDER_ELEVATOR_LAYER.get());
        event.register((stack, tintIndex) -> 0xFFFFFF,
                dev.celli.enderutilities.registry.ModItems.PORTAL_PANEL.get());
        event.register((stack, tintIndex) -> {
            if (tintIndex != 1 || !(stack.getItem() instanceof dev.celli.enderutilities.item.ItemEnderBucket bucket)) {
                return 0xFFFFFFFF;
            }
            var fluid = bucket.getFluidCached(stack);
            if (fluid.isEmpty()) {
                return 0xFFFFFFFF;
            }
            return net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid())
                    .getTintColor(fluid);
        }, dev.celli.enderutilities.registry.ModItems.ENDER_BUCKET.get());
    }

    private void onRegisterGeometryLoaders(net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "ender_tool"),
                dev.celli.enderutilities.client.renderer.model.EnderToolModel.Loader.INSTANCE
        );
        event.register(
                ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "ender_bucket"),
                dev.celli.enderutilities.client.renderer.model.EnderBucketModel.Loader.INSTANCE
        );
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_MODE);
        event.register(ACTIVATE_UNSELECTED);
    }

    private void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(dev.celli.enderutilities.registry.ModMenus.TOOL_WORKSTATION.get(), ToolWorkstationScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.ENDER_INFUSER.get(), EnderInfuserScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.NULLIFIER.get(), NullifierScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.QUICK_STACKER.get(), QuickStackerScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.HANDY_BAG.get(), HandyBagScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.PICKUP_MANAGER.get(), PickupManagerScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.INVENTORY_SWAPPER.get(), InventorySwapperScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.ASU.get(), AsuScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.BARREL.get(), BarrelScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.DRAWBRIDGE.get(), DrawbridgeScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.ENDER_FURNACE.get(), EnderFurnaceScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.INSERTER.get(), InserterScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.CREATION_STATION.get(), CreationStationScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.QUICK_STACKER_ADVANCED.get(), QuickStackerAdvancedScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.MSU.get(), MsuScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.SOUND_BLOCK.get(), SoundBlockScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.MEMORY_CHEST.get(), MemoryChestScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.HANDY_CHEST.get(), HandyChestScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.JSU.get(), JsuScreen::new);
        event.register(dev.celli.enderutilities.registry.ModMenus.PORTAL_PANEL.get(), PortalPanelScreen::new);
    }

    private void onRegisterRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(dev.celli.enderutilities.registry.ModBlockEntities.ENERGY_BRIDGE.get(),
                dev.celli.enderutilities.client.renderer.EnergyBridgeRenderer::new);
        event.registerEntityRenderer(dev.celli.enderutilities.registry.ModEntities.ENDER_PEARL_REUSABLE.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(dev.celli.enderutilities.registry.ModEntities.ENDER_ARROW.get(),
                dev.celli.enderutilities.client.renderer.EnderArrowRenderer::new);
        event.registerEntityRenderer(dev.celli.enderutilities.registry.ModEntities.CHAIR.get(),
                dev.celli.enderutilities.client.renderer.ChairRenderer::new);
        event.registerEntityRenderer(dev.celli.enderutilities.registry.ModEntities.ENDERMAN_FIGHTER.get(),
                dev.celli.enderutilities.client.renderer.EndermanFighterRenderer::new);
        event.registerBlockEntityRenderer(dev.celli.enderutilities.registry.ModBlockEntities.BARREL.get(),
                dev.celli.enderutilities.client.renderer.BarrelRenderer::new);
        event.registerBlockEntityRenderer(dev.celli.enderutilities.registry.ModBlockEntities.DRAWBRIDGE.get(),
                dev.celli.enderutilities.client.renderer.CamouflageBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(dev.celli.enderutilities.registry.ModBlockEntities.PORTAL_FRAME.get(),
                dev.celli.enderutilities.client.renderer.CamouflageBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(dev.celli.enderutilities.registry.ModBlockEntities.ELEVATOR.get(),
                dev.celli.enderutilities.client.renderer.CamouflageBlockEntityRenderer::new);
    }
}
