package dev.celli.enderutilities.network;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.blockentity.GuiActionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GuiActionPayload(BlockPos pos, int action, int element) implements CustomPacketPayload {
    public static final Type<GuiActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "gui_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuiActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GuiActionPayload::pos,
            ByteBufCodecs.VAR_INT, GuiActionPayload::action,
            ByteBufCodecs.VAR_INT, GuiActionPayload::element,
            GuiActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GuiActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.level().getBlockEntity(payload.pos()) instanceof GuiActionHandler handler
                    && player.distanceToSqr(payload.pos().getCenter()) <= 64.0) {
                handler.performGuiAction(player, payload.action(), payload.element());
                if (player.level().getBlockEntity(payload.pos()) instanceof BlockEntity be) {
                    be.setChanged();
                }
            }
        });
    }
}
