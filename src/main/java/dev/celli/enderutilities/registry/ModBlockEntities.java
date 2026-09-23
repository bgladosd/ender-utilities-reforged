package dev.celli.enderutilities.registry;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.blockentity.EnderInfuserBlockEntity;
import dev.celli.enderutilities.blockentity.EnergyBridgeBlockEntity;
import dev.celli.enderutilities.blockentity.ToolWorkstationBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnderUtilities.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToolWorkstationBlockEntity>> TOOL_WORKSTATION =
            BLOCK_ENTITIES.register("tool_workstation", () -> BlockEntityType.Builder.of(
                    ToolWorkstationBlockEntity::new,
                    ModBlocks.TOOL_WORKSTATION.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyBridgeBlockEntity>> ENERGY_BRIDGE =
            BLOCK_ENTITIES.register("energy_bridge", () -> BlockEntityType.Builder.of(
                    EnergyBridgeBlockEntity::new,
                    ModBlocks.ENERGY_BRIDGE_RESONATOR.get(),
                    ModBlocks.ENERGY_BRIDGE_RECEIVER.get(),
                    ModBlocks.ENERGY_BRIDGE_TRANSMITTER.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderInfuserBlockEntity>> ENDER_INFUSER =
            BLOCK_ENTITIES.register("ender_infuser", () -> BlockEntityType.Builder.of(
                    EnderInfuserBlockEntity::new,
                    ModBlocks.ENDER_INFUSER.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.ElevatorBlockEntity>> ELEVATOR =
            BLOCK_ENTITIES.register("ender_elevator", () -> BlockEntityType.Builder.of(
                    dev.celli.enderutilities.blockentity.ElevatorBlockEntity::new,
                    ModBlocks.ENDER_ELEVATOR.get(),
                    ModBlocks.ENDER_ELEVATOR_SLAB.get(),
                    ModBlocks.ENDER_ELEVATOR_LAYER.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.AsuBlockEntity>> ASU =
            BLOCK_ENTITIES.register("asu", () -> BlockEntityType.Builder.of(
                    dev.celli.enderutilities.blockentity.AsuBlockEntity::new, ModBlocks.ASU.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.BarrelBlockEntity>> BARREL =
            BLOCK_ENTITIES.register("barrel", () -> BlockEntityType.Builder.of(
                    dev.celli.enderutilities.blockentity.BarrelBlockEntity::new, ModBlocks.BARREL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.DrawbridgeBlockEntity>> DRAWBRIDGE =
            BLOCK_ENTITIES.register("draw_bridge", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new dev.celli.enderutilities.blockentity.DrawbridgeBlockEntity(pos, state,
                            state.is(ModBlocks.DRAW_BRIDGE_ADVANCED.get())),
                    ModBlocks.DRAW_BRIDGE.get(), ModBlocks.DRAW_BRIDGE_ADVANCED.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.EnderFurnaceBlockEntity>> ENDER_FURNACE =
            BLOCK_ENTITIES.register("ender_furnace", () -> BlockEntityType.Builder.of(
                    dev.celli.enderutilities.blockentity.EnderFurnaceBlockEntity::new, ModBlocks.ENDER_FURNACE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.InserterBlockEntity>> INSERTER =
            BLOCK_ENTITIES.register("inserter", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new dev.celli.enderutilities.blockentity.InserterBlockEntity(pos, state,
                            state.is(ModBlocks.INSERTER_FILTERED.get())),
                    ModBlocks.INSERTER.get(), ModBlocks.INSERTER_FILTERED.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.CreationStationBlockEntity>> CREATION_STATION =
            BLOCK_ENTITIES.register("creation_station", () -> BlockEntityType.Builder.of(
                    dev.celli.enderutilities.blockentity.CreationStationBlockEntity::new, ModBlocks.CREATION_STATION.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.QuickStackerAdvancedBlockEntity>> QUICK_STACKER_ADVANCED =
            BLOCK_ENTITIES.register("quick_stacker_advanced", () -> BlockEntityType.Builder.of(
                    dev.celli.enderutilities.blockentity.QuickStackerAdvancedBlockEntity::new,
                    ModBlocks.QUICK_STACKER_ADVANCED.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.MsuBlockEntity>> MSU =
            BLOCK_ENTITIES.register("msu", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new dev.celli.enderutilities.blockentity.MsuBlockEntity(pos, state,
                            state.is(ModBlocks.MSU_BUNDLE.get()) ? 1 : 0),
                    ModBlocks.MSU.get(), ModBlocks.MSU_BUNDLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.SoundBlockEntity>> SOUND_BLOCK =
            BLOCK_ENTITIES.register("sound_block", () -> BlockEntityType.Builder.of(
                    dev.celli.enderutilities.blockentity.SoundBlockEntity::new, ModBlocks.SOUND_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.MemoryChestBlockEntity>> MEMORY_CHEST =
            BLOCK_ENTITIES.register("memory_chest", () -> BlockEntityType.Builder.of(
                    (pos, state) -> {
                        int tier = state.is(ModBlocks.MEMORY_CHEST_2.get()) ? 2 : state.is(ModBlocks.MEMORY_CHEST_1.get()) ? 1 : 0;
                        return new dev.celli.enderutilities.blockentity.MemoryChestBlockEntity(pos, state, tier);
                    },
                    ModBlocks.MEMORY_CHEST_0.get(), ModBlocks.MEMORY_CHEST_1.get(), ModBlocks.MEMORY_CHEST_2.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.HandyChestBlockEntity>> HANDY_CHEST =
            BLOCK_ENTITIES.register("handy_chest", () -> BlockEntityType.Builder.of(
                    (pos, state) -> {
                        int tier = state.is(ModBlocks.HANDY_CHEST_3.get()) ? 3
                                : state.is(ModBlocks.HANDY_CHEST_2.get()) ? 2
                                : state.is(ModBlocks.HANDY_CHEST_1.get()) ? 1 : 0;
                        return new dev.celli.enderutilities.blockentity.HandyChestBlockEntity(pos, state, tier);
                    },
                    ModBlocks.HANDY_CHEST_0.get(), ModBlocks.HANDY_CHEST_1.get(),
                    ModBlocks.HANDY_CHEST_2.get(), ModBlocks.HANDY_CHEST_3.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.JsuBlockEntity>> JSU =
            BLOCK_ENTITIES.register("jsu", () -> BlockEntityType.Builder.of(
                    dev.celli.enderutilities.blockentity.JsuBlockEntity::new, ModBlocks.JSU.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.PortalBlockEntity>> PORTAL =
            BLOCK_ENTITIES.register("portal", () -> BlockEntityType.Builder.of(
                    dev.celli.enderutilities.blockentity.PortalBlockEntity::new, ModBlocks.PORTAL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.PortalPanelBlockEntity>> PORTAL_PANEL =
            BLOCK_ENTITIES.register("portal_panel", () -> BlockEntityType.Builder.of(
                    dev.celli.enderutilities.blockentity.PortalPanelBlockEntity::new, ModBlocks.PORTAL_PANEL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.celli.enderutilities.blockentity.PortalFrameBlockEntity>> PORTAL_FRAME =
            BLOCK_ENTITIES.register("portal_frame", () -> BlockEntityType.Builder.of(
                    dev.celli.enderutilities.blockentity.PortalFrameBlockEntity::new, ModBlocks.PORTAL_FRAME.get()).build(null));

    private ModBlockEntities() {
    }
}
