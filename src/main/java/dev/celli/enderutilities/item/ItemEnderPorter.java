package dev.celli.enderutilities.item;

import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.util.EntityUtils;
import dev.celli.enderutilities.util.TeleportUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ItemEnderPorter extends ItemLocationBoundModular {
    public static final int ENDER_CHARGE_COST_INTER_DIM_TP = 5000;
    public static final int ENDER_CHARGE_COST_CROSS_DIM_TP = 25000;
    private static final int USE_TIME = 40;

    private final boolean advanced;

    public ItemEnderPorter(Properties properties, boolean advanced) {
        super(properties.stacksTo(1));
        this.advanced = advanced;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    @Override
    public boolean useBindLocking(ItemStack stack) {
        return true;
    }

    @Override
    protected String commonTooltipKey() {
        return "enderutilities.tooltips.itemlocationboundmodular";
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (player.level().isClientSide || !player.isShiftKeyDown()
                || !ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)) {
            return false;
        }
        return doTeleport(stack, entity, true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.isShiftKeyDown()) {
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, true);
            if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult) {
                return InteractionResultHolder.pass(stack);
            }
        }

        TargetLocation target = ModularHelper.getSelectedTarget(stack);
        if (target != null
                && !EntityUtils.doesEntityStackHaveBlacklistedEntities(player)
                && (advanced || target.dimension().equals(player.level().dimension()))) {
            int cost = target.dimension().equals(player.level().dimension())
                    ? ENDER_CHARGE_COST_INTER_DIM_TP
                    : ENDER_CHARGE_COST_CROSS_DIM_TP;
            if (!ModularHelper.useEnderCharge(stack, cost, true)) {
                return InteractionResultHolder.pass(stack);
            }
            player.startUsingItem(hand);
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PORTAL_TRIGGER, SoundSource.MASTER, 0.08F, 1.2F);
            }
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!(living instanceof Player player)
                || !ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)) {
            return;
        }
        int useTime = player.getAbilities().instabuild ? USE_TIME >> 2 : USE_TIME;
        if ((getUseDuration(stack, living) - timeLeft) >= useTime) {
            doTeleport(stack, player, false);
        }
    }

    private boolean doTeleport(ItemStack stack, Entity entity, boolean targetMustSneakIfPlayer) {
        TargetLocation target = ModularHelper.getSelectedTarget(stack);
        if (target == null
                || EntityUtils.doesEntityStackHaveBlacklistedEntities(entity)
                || (!advanced && !target.dimension().equals(entity.level().dimension()))) {
            return false;
        }
        int cost = target.dimension().equals(entity.level().dimension())
                ? ENDER_CHARGE_COST_INTER_DIM_TP
                : ENDER_CHARGE_COST_CROSS_DIM_TP;
        if (!ModularHelper.useEnderCharge(stack, cost, true)) {
            return false;
        }
        if (targetMustSneakIfPlayer && entity instanceof Player player && !player.isShiftKeyDown()) {
            return false;
        }
        ModularHelper.useEnderCharge(stack, cost, false);
        TeleportUtil.teleportEntityUsingModularItem(entity, stack, true, true);
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 10;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType moduleType) {
        return switch (moduleType) {
            case ENDERCAPACITOR -> 1;
            case LINKCRYSTAL -> 9;
            default -> 0;
        };
    }

    @Override
    public int getMaxModules(ItemStack container, ItemStack moduleStack) {
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof IModule module)) {
            return 0;
        }
        return getMaxModules(container, module.getModuleType(moduleStack));
    }
}
