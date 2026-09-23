package dev.celli.enderutilities.network;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.menu.ItemGuiActionMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ItemGuiActionPayload(int action, int element) implements CustomPacketPayload {
    public static final Type<ItemGuiActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "item_gui_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemGuiActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ItemGuiActionPayload::action,
            ByteBufCodecs.VAR_INT, ItemGuiActionPayload::element,
            ItemGuiActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ItemGuiActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.containerMenu instanceof ItemGuiActionMenu menu) {
                menu.performItemGuiAction(player, payload.action(), payload.element());
            }
        });
    }
}
