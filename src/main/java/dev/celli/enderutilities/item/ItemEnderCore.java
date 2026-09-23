package dev.celli.enderutilities.item;

import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemEnderCore extends ItemEnderUtilities implements IModule {
    private final int coreTier;
    private final boolean active;

    public ItemEnderCore(Properties properties, int coreTier, boolean active) {
        super(properties);
        this.coreTier = coreTier;
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public ItemStack activate(ItemStack stack) {
        if (active) {
            return stack;
        }
        var activeItem = switch (coreTier) {
            case 2 -> ModItems.ENDER_CORE_ADVANCED_ACTIVE.get();
            case 1 -> ModItems.ENDER_CORE_ENHANCED_ACTIVE.get();
            default -> ModItems.ENDER_CORE_BASIC_ACTIVE.get();
        };
        return new ItemStack(activeItem, stack.getCount());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (dev.celli.enderutilities.util.EndCrystalInteractions.tryUseHeldItem(player, hand)) {
            return ItemEnderUtilities.handledWithoutUseAnim(player.getItemInHand(hand), level.isClientSide);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && dev.celli.enderutilities.util.EndCrystalInteractions.tryUseHeldItem(player, context.getHand())) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public ModuleType getModuleType(ItemStack stack) {
        return ModuleType.ENDERCORE;
    }

    @Override
    public int getModuleTier(ItemStack stack) {
        return active ? coreTier : 10 + coreTier;
    }
}
