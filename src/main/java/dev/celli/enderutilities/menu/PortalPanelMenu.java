package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.PortalPanelBlockEntity;
import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class PortalPanelMenu extends EnderMenu {
    private final PortalPanelBlockEntity panel;
    private final ContainerData data;

    public PortalPanelMenu(int id, Inventory playerInventory, PortalPanelBlockEntity panel) {
        this(id, playerInventory, panel, new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> panel.getActiveTargetId();
                    case 1 -> ModCommonConfig.PORTAL_ONLY_ALLOWS_PORTAL_TYPE_LINK_CRYSTALS.get() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    panel.setActiveTargetId(value);
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        });
    }

    public PortalPanelMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, PortalPanelBlockEntity.class),
                new SimpleContainerData(2));
    }

    private PortalPanelMenu(int id, Inventory playerInventory, PortalPanelBlockEntity panel, ContainerData data) {
        super(ModMenus.PORTAL_PANEL.get(), id);
        this.panel = panel;
        this.data = data;
        int posX = 53;
        int posY = 19;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 4; c++) {
                addSlot(new SlotItemHandler(panel.getInventory(), r * 4 + c, posX + c * 18, posY + r * 50));
            }
        }
        posY = 37;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 4; c++) {
                addSlot(new SlotItemHandler(panel.getInventory(), r * 4 + c + 8, posX + c * 18, posY + r * 50));
            }
        }
        addPlayerSlots(playerInventory, 8, 168);
        addDataSlots(data);
    }

    public PortalPanelBlockEntity getPanel() {
        return panel;
    }

    public int getActiveTargetId() {
        return data.get(0);
    }

    public boolean portalOnly() {
        return data.get(1) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, 16);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(panel, player);
    }
}
