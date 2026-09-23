package dev.celli.enderutilities.network;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemHandyBag;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenHandyBagPayload() implements CustomPacketPayload {
    public static final Type<OpenHandyBagPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "open_handy_bag"));

    public static final StreamCodec<ByteBuf, OpenHandyBagPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenHandyBagPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenHandyBagPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            int slot = ItemHandyBag.findOpenableBagSlot(player);
            if (slot < 0) {
                return;
            }
            ItemStack bag = player.getInventory().getItem(slot);
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new dev.celli.enderutilities.menu.HandyBagMenu(id, inv, slot),
                    bag.getHoverName()
            ), buf -> buf.writeVarInt(slot));
        });
    }
}
