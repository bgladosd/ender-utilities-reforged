package dev.celli.enderutilities.network;

import org.apache.commons.lang3.StringUtils;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.blockentity.ToolWorkstationBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RenamePayload(String name) implements CustomPacketPayload {
    public static final Type<RenamePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "rename"));

    public static final StreamCodec<ByteBuf, RenamePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RenamePayload::name, RenamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RenamePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof dev.celli.enderutilities.menu.ToolWorkstationMenu menu) {
                ToolWorkstationBlockEntity workstation = menu.getWorkstation();
                ItemStack stack = workstation.getInventory().getStackInSlot(ToolWorkstationBlockEntity.SLOT_RENAME);
                if (stack.isEmpty()) {
                    return;
                }
                if (StringUtils.isBlank(payload.name())) {
                    stack.remove(DataComponents.CUSTOM_NAME);
                } else {
                    stack.set(DataComponents.CUSTOM_NAME, Component.literal(payload.name()));
                }
                workstation.getInventory().setStackInSlot(ToolWorkstationBlockEntity.SLOT_RENAME, stack);
            } else if (context.player().containerMenu instanceof dev.celli.enderutilities.menu.PortalPanelMenu panelMenu) {
                panelMenu.getPanel().setTargetName(payload.name());
            } else {
                ItemStack held = context.player().getMainHandItem();
                if (held.getItem() instanceof dev.celli.enderutilities.item.ItemBuildersWand) {
                    var template = held.getOrDefault(
                            dev.celli.enderutilities.registry.ModDataComponents.WAND_TEMPLATE,
                            dev.celli.enderutilities.data.WandTemplate.EMPTY);
                    held.set(dev.celli.enderutilities.registry.ModDataComponents.WAND_TEMPLATE, template.withName(payload.name()));
                }
            }
        });
    }
}
