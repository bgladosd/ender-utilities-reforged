package dev.celli.enderutilities.network;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.module.IKeyBound;
import dev.celli.enderutilities.item.module.IKeyBoundUnselected;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record KeybindPayload(int key) implements CustomPacketPayload {
    public static final Type<KeybindPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "keybind"));

    public static final StreamCodec<ByteBuf, KeybindPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, KeybindPayload::key, KeybindPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KeybindPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof IKeyBound keyBound && keyBound.doKeyBindingAction(player, stack, payload.key())) {
                    player.setItemInHand(hand, stack);
                    return;
                }
            }
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof IKeyBoundUnselected unbound
                        && unbound.doUnselectedKeyAction(player, stack, payload.key())) {
                    player.getInventory().setItem(i, stack);
                    return;
                }
            }
        });
    }
}
