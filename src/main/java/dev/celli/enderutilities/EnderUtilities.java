package dev.celli.enderutilities;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.item.ItemEnderBucket;
import dev.celli.enderutilities.network.KeybindPayload;
import dev.celli.enderutilities.registry.ModBlockEntities;
import dev.celli.enderutilities.registry.ModBlocks;
import dev.celli.enderutilities.registry.ModCreativeTabs;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.registry.ModEntities;
import dev.celli.enderutilities.registry.ModItems;
import dev.celli.enderutilities.registry.ModMenus;
import dev.celli.enderutilities.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(EnderUtilities.MOD_ID)
public class EnderUtilities {
    public static final String MOD_ID = "enderutilities";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EnderUtilities(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerAttributes);

        ModDataComponents.COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, dev.celli.enderutilities.config.ModClientConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Ender Utilities Reforged loaded");
    }

    private void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ENDER_INFUSER.get(),
                (be, side) -> be.getHopperHandler()
        );
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ASU.get(), (be, side) -> be.getHopperHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.BARREL.get(), (be, side) -> be.getHopperHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.DRAWBRIDGE.get(), (be, side) -> be.getHopperHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ENDER_FURNACE.get(), (be, side) -> be.getHopperHandler(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.INSERTER.get(), (be, side) -> be.getHopperHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CREATION_STATION.get(), (be, side) -> be.getHopperHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MSU.get(), (be, side) -> be.getHopperHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MEMORY_CHEST.get(), (be, side) -> be.getHopperHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.HANDY_CHEST.get(), (be, side) -> be.getHopperHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.JSU.get(), (be, side) -> be.getHopperHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.PORTAL_PANEL.get(), (be, side) -> be.getHopperHandler());
        event.registerItem(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> stack.getItem() instanceof ItemEnderBucket bucket
                        ? new dev.celli.enderutilities.util.fluid.FluidHandlerEnderBucket(bucket, stack)
                        : null,
                ModItems.ENDER_BUCKET.get()
        );
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(KeybindPayload.TYPE, KeybindPayload.STREAM_CODEC, KeybindPayload::handle);
        registrar.playToServer(dev.celli.enderutilities.network.RenamePayload.TYPE,
                dev.celli.enderutilities.network.RenamePayload.STREAM_CODEC,
                dev.celli.enderutilities.network.RenamePayload::handle);
        registrar.playToServer(dev.celli.enderutilities.network.GuiActionPayload.TYPE,
                dev.celli.enderutilities.network.GuiActionPayload.STREAM_CODEC,
                dev.celli.enderutilities.network.GuiActionPayload::handle);
        registrar.playToServer(dev.celli.enderutilities.network.OpenHandyBagPayload.TYPE,
                dev.celli.enderutilities.network.OpenHandyBagPayload.STREAM_CODEC,
                dev.celli.enderutilities.network.OpenHandyBagPayload::handle);
        registrar.playToServer(dev.celli.enderutilities.network.ItemGuiActionPayload.TYPE,
                dev.celli.enderutilities.network.ItemGuiActionPayload.STREAM_CODEC,
                dev.celli.enderutilities.network.ItemGuiActionPayload::handle);
    }

    private void registerAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ModEntities.ENDERMAN_FIGHTER.get(),
                dev.celli.enderutilities.entity.EntityEndermanFighter.createAttributes().build());
    }
}
