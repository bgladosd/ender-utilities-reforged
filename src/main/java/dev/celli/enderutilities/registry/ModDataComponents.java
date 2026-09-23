package dev.celli.enderutilities.registry;

import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.data.ModularItemData;
import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.data.TargetLocation;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EnderUtilities.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TargetLocation>> TARGET =
            register("target", builder -> builder.persistent(TargetLocation.CODEC).networkSynchronized(TargetLocation.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OwnerInfo>> OWNER =
            register("owner", builder -> builder.persistent(OwnerInfo.CODEC).networkSynchronized(OwnerInfo.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ModularItemData>> MODULAR =
            register("modular", builder -> builder.persistent(ModularItemData.CODEC).networkSynchronized(ModularItemData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENDER_CHARGE =
            register("ender_charge", builder -> builder.persistent(net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.celli.enderutilities.data.PetContractData>> PET_CONTRACT =
            register("pet_contract", builder -> builder.persistent(dev.celli.enderutilities.data.PetContractData.CODEC)
                    .networkSynchronized(dev.celli.enderutilities.data.PetContractData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.celli.enderutilities.data.HarnessTarget>> HARNESS_TARGET =
            register("harness_target", builder -> builder.persistent(dev.celli.enderutilities.data.HarnessTarget.CODEC)
                    .networkSynchronized(dev.celli.enderutilities.data.HarnessTarget.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.BlockPos>> RULER_START =
            register("ruler_start", builder -> builder.persistent(net.minecraft.core.BlockPos.CODEC)
                    .networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.BlockPos>> RULER_END =
            register("ruler_end", builder -> builder.persistent(net.minecraft.core.BlockPos.CODEC)
                    .networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.DyeColor>> ELEVATOR_COLOR =
            register("elevator_color", builder -> builder.persistent(net.minecraft.world.item.DyeColor.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.idMapper(
                            id -> net.minecraft.world.item.DyeColor.byId(id),
                            net.minecraft.world.item.DyeColor::getId)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.celli.enderutilities.data.PortalScale>> PORTAL_SCALE =
            register("portal_scale", builder -> builder.persistent(dev.celli.enderutilities.data.PortalScale.CODEC)
                    .networkSynchronized(dev.celli.enderutilities.data.PortalScale.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.celli.enderutilities.data.ItemInventoryData>> ITEM_INVENTORY =
            register("item_inventory", builder -> builder.persistent(dev.celli.enderutilities.data.ItemInventoryData.CODEC)
                    .networkSynchronized(dev.celli.enderutilities.data.ItemInventoryData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.celli.enderutilities.data.CarriedBlock>> CARRIED_BLOCK =
            register("carried_block", builder -> builder.persistent(dev.celli.enderutilities.data.CarriedBlock.CODEC)
                    .networkSynchronized(dev.celli.enderutilities.data.CarriedBlock.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.celli.enderutilities.data.StoredEntities>> STORED_ENTITIES =
            register("stored_entities", builder -> builder.persistent(dev.celli.enderutilities.data.StoredEntities.CODEC)
                    .networkSynchronized(dev.celli.enderutilities.data.StoredEntities.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ITEM_MODE =
            register("item_mode", builder -> builder.persistent(net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOOL_DROPS =
            register("tool_drops", builder -> builder.persistent(net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CREATIVE_BREAKING =
            register("creative_breaking", builder -> builder.persistent(Codec.BOOL)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHAIR_WIDTH =
            register("chair_width", builder -> builder.persistent(net.minecraft.util.ExtraCodecs.POSITIVE_INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHAIR_HEIGHT =
            register("chair_height", builder -> builder.persistent(net.minecraft.util.ExtraCodecs.POSITIVE_INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BUCKET_FLUID =
            register("bucket_fluid", builder -> builder.persistent(Codec.STRING)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BUCKET_AMOUNT =
            register("bucket_amount", builder -> builder.persistent(net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BUCKET_LINKED =
            register("bucket_linked", builder -> builder.persistent(Codec.BOOL)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BUCKET_CACHED_CAPACITY =
            register("bucket_cached_capacity", builder -> builder.persistent(net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.celli.enderutilities.data.WandTemplate>> WAND_TEMPLATE =
            register("wand_template", builder -> builder.persistent(dev.celli.enderutilities.data.WandTemplate.CODEC)
                    .networkSynchronized(dev.celli.enderutilities.data.WandTemplate.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.BlockPos>> WAND_MOVE_ORIGIN =
            register("wand_move_origin", builder -> builder.persistent(net.minecraft.core.BlockPos.CODEC)
                    .networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.celli.enderutilities.data.SlotMaskPresets>> SLOT_MASKS =
            register("slot_masks", builder -> builder.persistent(dev.celli.enderutilities.data.SlotMaskPresets.CODEC)
                    .networkSynchronized(dev.celli.enderutilities.data.SlotMaskPresets.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.celli.enderutilities.data.WandModeOptions>> WAND_OPTIONS =
            register("wand_options", builder -> builder.persistent(dev.celli.enderutilities.data.WandModeOptions.CODEC)
                    .networkSynchronized(dev.celli.enderutilities.data.WandModeOptions.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.celli.enderutilities.data.RulerCardData>> RULER_CARD =
            register("ruler_card", builder -> builder.persistent(dev.celli.enderutilities.data.RulerCardData.CODEC)
                    .networkSynchronized(dev.celli.enderutilities.data.RulerCardData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.celli.enderutilities.data.PlacementPropertyValues>> PLACEMENT_PROPERTIES =
            register("placement_properties", builder -> builder.persistent(dev.celli.enderutilities.data.PlacementPropertyValues.CODEC)
                    .networkSynchronized(dev.celli.enderutilities.data.PlacementPropertyValues.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CHUNK_LOADING_REQUIRED =
            register("chunk_loading_required", builder -> builder.persistent(Codec.BOOL)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name,
            UnaryOperator<DataComponentType.Builder<T>> builder
    ) {
        return COMPONENTS.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }

    private ModDataComponents() {
    }
}
