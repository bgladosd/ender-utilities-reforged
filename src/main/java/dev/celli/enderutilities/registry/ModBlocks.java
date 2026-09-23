package dev.celli.enderutilities.registry;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.block.EnderInfuserBlock;
import dev.celli.enderutilities.block.EnergyBridgeBlock;
import dev.celli.enderutilities.block.HorizontalMachineBlock;
import dev.celli.enderutilities.block.ToolWorkstationBlock;
import dev.celli.enderutilities.blockentity.EnergyBridgeBlockEntity;
import dev.celli.enderutilities.item.ItemBlockEnderUtilities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnderUtilities.MOD_ID);

    public static final DeferredBlock<EnderInfuserBlock> ENDER_INFUSER = BLOCKS.register("ender_infuser",
            () -> new EnderInfuserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(6.0F, 20.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<ToolWorkstationBlock> TOOL_WORKSTATION = BLOCKS.register("tool_workstation",
            () -> new ToolWorkstationBlock(metal(6.0F, 20.0F)));

    public static final DeferredBlock<EnergyBridgeBlock> ENERGY_BRIDGE_RESONATOR = BLOCKS.register("energy_bridge_resonator",
            () -> energyBridge(EnergyBridgeBlockEntity.Type.RESONATOR));
    public static final DeferredBlock<EnergyBridgeBlock> ENERGY_BRIDGE_RECEIVER = BLOCKS.register("energy_bridge_receiver",
            () -> energyBridge(EnergyBridgeBlockEntity.Type.RECEIVER));
    public static final DeferredBlock<EnergyBridgeBlock> ENERGY_BRIDGE_TRANSMITTER = BLOCKS.register("energy_bridge_transmitter",
            () -> energyBridge(EnergyBridgeBlockEntity.Type.TRANSMITTER));

    public static final DeferredBlock<dev.celli.enderutilities.block.ElevatorBlock> ENDER_ELEVATOR = BLOCKS.register("ender_elevator",
            () -> new dev.celli.enderutilities.block.ElevatorBlock(elevatorProperties(),
                    dev.celli.enderutilities.block.ElevatorBlock.Kind.FULL));
    public static final DeferredBlock<dev.celli.enderutilities.block.ElevatorBlock> ENDER_ELEVATOR_SLAB = BLOCKS.register("ender_elevator_slab",
            () -> new dev.celli.enderutilities.block.ElevatorBlock(elevatorProperties(),
                    dev.celli.enderutilities.block.ElevatorBlock.Kind.SLAB));
    public static final DeferredBlock<dev.celli.enderutilities.block.ElevatorBlock> ENDER_ELEVATOR_LAYER = BLOCKS.register("ender_elevator_layer",
            () -> new dev.celli.enderutilities.block.ElevatorBlock(elevatorProperties(),
                    dev.celli.enderutilities.block.ElevatorBlock.Kind.LAYER));

    public static final DeferredBlock<dev.celli.enderutilities.block.AsuBlock> ASU = BLOCKS.register("asu",
            () -> new dev.celli.enderutilities.block.AsuBlock(metal(6.0F, 20.0F)));
    public static final DeferredBlock<dev.celli.enderutilities.block.BarrelBlock> BARREL = BLOCKS.register("barrel",
            () -> new dev.celli.enderutilities.block.BarrelBlock(metal(4.0F, 10.0F)));
    public static final DeferredBlock<dev.celli.enderutilities.block.DrawbridgeBlock> DRAW_BRIDGE = BLOCKS.register("draw_bridge",
            () -> new dev.celli.enderutilities.block.DrawbridgeBlock(metal(4.0F, 10.0F), false));
    public static final DeferredBlock<dev.celli.enderutilities.block.DrawbridgeBlock> DRAW_BRIDGE_ADVANCED = BLOCKS.register("draw_bridge_advanced",
            () -> new dev.celli.enderutilities.block.DrawbridgeBlock(metal(4.0F, 10.0F), true));
    public static final DeferredBlock<dev.celli.enderutilities.block.EnderFurnaceBlock> ENDER_FURNACE = BLOCKS.register("ender_furnace",
            () -> new dev.celli.enderutilities.block.EnderFurnaceBlock(metal(6.0F, 20.0F)));
    public static final DeferredBlock<dev.celli.enderutilities.block.FloorBlock> FLOOR = BLOCKS.register("floor",
            () -> new dev.celli.enderutilities.block.FloorBlock(wood(), false));
    public static final DeferredBlock<dev.celli.enderutilities.block.FloorBlock> FLOOR_CRACKED = BLOCKS.register("floor_cracked",
            () -> new dev.celli.enderutilities.block.FloorBlock(wood(), true));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> INSERTER = BLOCKS.register("inserter",
            () -> new dev.celli.enderutilities.block.HorizontalMachineBlock(stone(2.0F, 6.0F).noOcclusion(),
                    dev.celli.enderutilities.block.HorizontalMachineBlock.Kind.INSERTER));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> INSERTER_FILTERED = BLOCKS.register("inserter_filtered",
            () -> new dev.celli.enderutilities.block.HorizontalMachineBlock(stone(2.0F, 6.0F).noOcclusion(),
                    dev.celli.enderutilities.block.HorizontalMachineBlock.Kind.INSERTER_FILTERED));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> CREATION_STATION = BLOCKS.register("creation_station",
            () -> new dev.celli.enderutilities.block.HorizontalMachineBlock(metal(6.0F, 20.0F).lightLevel(state -> 10),
                    dev.celli.enderutilities.block.HorizontalMachineBlock.Kind.CREATION_STATION));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> QUICK_STACKER_ADVANCED = BLOCKS.register("quick_stacker_advanced",
            () -> new dev.celli.enderutilities.block.HorizontalMachineBlock(metal(6.0F, 20.0F),
                    dev.celli.enderutilities.block.HorizontalMachineBlock.Kind.QUICK_STACKER_ADVANCED));
    public static final DeferredBlock<dev.celli.enderutilities.block.MolecularExciterBlock> MOLECULAR_EXCITER = BLOCKS.register("molecular_exciter",
            () -> new dev.celli.enderutilities.block.MolecularExciterBlock(stone(3.0F, 20.0F)));
    public static final DeferredBlock<dev.celli.enderutilities.block.MsuBlock> MSU = BLOCKS.register("msu",
            () -> new dev.celli.enderutilities.block.MsuBlock(metal(6.0F, 20.0F), 0));
    public static final DeferredBlock<dev.celli.enderutilities.block.MsuBlock> MSU_BUNDLE = BLOCKS.register("msu_bundle",
            () -> new dev.celli.enderutilities.block.MsuBlock(metal(6.0F, 20.0F), 1));
    public static final DeferredBlock<dev.celli.enderutilities.block.PhasingBlock> PHASING = BLOCKS.register("phasing",
            () -> new dev.celli.enderutilities.block.PhasingBlock(stone(2.0F, 10.0F).noOcclusion(), false));
    public static final DeferredBlock<dev.celli.enderutilities.block.PhasingBlock> PHASING_INVERTED = BLOCKS.register("phasing_inverted",
            () -> new dev.celli.enderutilities.block.PhasingBlock(stone(2.0F, 10.0F).noOcclusion(), true));
    public static final DeferredBlock<dev.celli.enderutilities.block.PortalBlock> PORTAL = BLOCKS.register("portal",
            () -> new dev.celli.enderutilities.block.PortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .noOcclusion()
                    .strength(-1.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 11)
                    .noLootTable()));
    public static final DeferredBlock<dev.celli.enderutilities.block.PortalFrameBlock> PORTAL_FRAME = BLOCKS.register("frame",
            () -> new dev.celli.enderutilities.block.PortalFrameBlock(stone(4.0F, 20.0F)));
    public static final DeferredBlock<dev.celli.enderutilities.block.PortalPanelBlock> PORTAL_PANEL = BLOCKS.register("portal_panel",
            () -> new dev.celli.enderutilities.block.PortalPanelBlock(stone(4.0F, 20.0F).noOcclusion()));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> SOUND_BLOCK = BLOCKS.register("sound_block",
            () -> new dev.celli.enderutilities.block.HorizontalMachineBlock(stone(4.0F, 10.0F),
                    dev.celli.enderutilities.block.HorizontalMachineBlock.Kind.SOUND_BLOCK));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> MEMORY_CHEST_0 = BLOCKS.register("memory_chest_0",
            () -> chest(HorizontalMachineBlock.Kind.MEMORY_CHEST_0));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> MEMORY_CHEST_1 = BLOCKS.register("memory_chest_1",
            () -> chest(HorizontalMachineBlock.Kind.MEMORY_CHEST_1));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> MEMORY_CHEST_2 = BLOCKS.register("memory_chest_2",
            () -> chest(HorizontalMachineBlock.Kind.MEMORY_CHEST_2));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> HANDY_CHEST_0 = BLOCKS.register("handy_chest_0",
            () -> chest(HorizontalMachineBlock.Kind.HANDY_CHEST_0));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> HANDY_CHEST_1 = BLOCKS.register("handy_chest_1",
            () -> chest(HorizontalMachineBlock.Kind.HANDY_CHEST_1));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> HANDY_CHEST_2 = BLOCKS.register("handy_chest_2",
            () -> chest(HorizontalMachineBlock.Kind.HANDY_CHEST_2));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> HANDY_CHEST_3 = BLOCKS.register("handy_chest_3",
            () -> chest(HorizontalMachineBlock.Kind.HANDY_CHEST_3));
    public static final DeferredBlock<dev.celli.enderutilities.block.HorizontalMachineBlock> JSU = BLOCKS.register("jsu",
            () -> new dev.celli.enderutilities.block.HorizontalMachineBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(6.0F, 60.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(),
                    HorizontalMachineBlock.Kind.JSU));

    private static BlockBehaviour.Properties elevatorProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOL)
                .strength(4.0F, 10.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties metal(float hardness, float resistance) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(hardness, resistance)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties stone(float hardness, float resistance) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(hardness, resistance)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0F, 6.0F)
                .sound(SoundType.WOOD)
                .noOcclusion();
    }

    private static HorizontalMachineBlock chest(HorizontalMachineBlock.Kind kind) {
        return new HorizontalMachineBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(6.0F, 60.0F)
                .sound(SoundType.WOOD)
                .requiresCorrectToolForDrops()
                .noOcclusion(), kind);
    }

    private static EnergyBridgeBlock energyBridge(EnergyBridgeBlockEntity.Type type) {
        return new EnergyBridgeBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(8.0F, 20.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> 15), type);
    }

    public static DeferredItem<BlockItem> registerBlockItem(DeferredRegister.Items items, DeferredBlock<?> block) {
        return items.register(block.getId().getPath(), () -> new ItemBlockEnderUtilities(block.get(), new Item.Properties()));
    }

    private ModBlocks() {
    }
}
