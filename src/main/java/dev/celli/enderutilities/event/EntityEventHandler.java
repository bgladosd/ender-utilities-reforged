package dev.celli.enderutilities.event;

import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.item.ItemEnderBag;
import dev.celli.enderutilities.item.ItemEnderLasso;
import dev.celli.enderutilities.item.ItemLivingManipulator;
import dev.celli.enderutilities.item.ItemMobHarness;
import dev.celli.enderutilities.item.ItemSyringe;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EndCrystalInteractions;
import dev.celli.enderutilities.util.EntityUtils;
import dev.celli.enderutilities.util.TeleportUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = dev.celli.enderutilities.EnderUtilities.MOD_ID)
public final class EntityEventHandler {
    private EntityEventHandler() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (handleLasso(event.getEntity(), event.getHand(), event.getTarget())
                || handleHarness(event.getEntity(), event.getHand(), event.getTarget())
                || handleManipulator(event.getEntity(), event.getHand(), event.getTarget())
                || EndCrystalInteractions.tryUseHeldItem(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (handleLasso(event.getEntity(), event.getHand(), event.getTarget())
                || handleHarness(event.getEntity(), event.getHand(), event.getTarget())
                || handleManipulator(event.getEntity(), event.getHand(), event.getTarget())
                || EndCrystalInteractions.tryUseHeldItem(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean handleLasso(Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.entity.Entity target) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ItemEnderLasso) || !(target instanceof LivingEntity)) {
            return false;
        }
        boolean client = player.level().isClientSide;
        if (ModCommonConfig.ENDER_LASSO_ALLOW_PLAYERS.getAsBoolean() || !TeleportUtil.doesEntityStackHavePlayers(target)) {
            if (ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)
                    && ModularHelper.useEnderCharge(stack, ItemEnderLasso.ENDER_CHARGE_COST, false)) {
                if (target instanceof Mob mob
                        && ModularHelper.getInstalledModuleCount(stack, ModuleType.MOBPERSISTENCE) > 0) {
                    EntityUtils.applyMobPersistence(mob);
                }
                return client || TeleportUtil.teleportEntityUsingModularItem(target, stack, true, true) != null;
            }
        }
        return false;
    }

    private static boolean handleHarness(Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.entity.Entity target) {
        ItemStack stack = player.getItemInHand(hand);
        // Orig: EntityInteract only when target instanceof EntityLivingBase (not frames/paintings/boats).
        if (!(stack.getItem() instanceof ItemMobHarness harness) || !(target instanceof LivingEntity)) {
            return false;
        }
        return harness.handleInteraction(stack, player, target);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            boolean hasGoal = mob.goalSelector.getAvailableGoals().stream()
                    .anyMatch(wrapped -> wrapped.getGoal() instanceof dev.celli.enderutilities.entity.ai.ControlledByHarnessGoal);
            if (!hasGoal) {
                mob.goalSelector.addGoal(1, new dev.celli.enderutilities.entity.ai.ControlledByHarnessGoal(mob));
            }
        }
    }

    private static boolean handleManipulator(Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.entity.Entity target) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ItemLivingManipulator manipulator) || !(target instanceof LivingEntity living)) {
            return false;
        }
        return manipulator.handleInteraction(stack, player, living) != InteractionResult.PASS;
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && ItemSyringe.isPassified(mob)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Mob mob && ItemSyringe.isPassified(mob) && event.getSource().getEntity() != null) {
            ItemSyringe.removePassifiedState(mob);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        dev.celli.enderutilities.event.tasks.PlayerTaskScheduler.getInstance().runTasks(player.level(), player);
        if (player.tickCount % 40 == 0) {
            refreshHeldChunkLoading(player);
        }
    }

    private static void refreshHeldChunkLoading(Player player) {
        ItemStack stack = heldChunkLoadingItem(player);
        if (stack.isEmpty() || !Boolean.TRUE.equals(stack.get(ModDataComponents.CHUNK_LOADING_REQUIRED))) {
            return;
        }
        var target = ModularHelper.getSelectedTarget(stack);
        if (target == null || !(player.level() instanceof net.minecraft.server.level.ServerLevel server)) {
            return;
        }
        long now = server.getGameTime();
        net.minecraft.server.level.ServerLevel dest = server.getServer().getLevel(target.dimension());
        if (dest != null) {
            now = dest.getGameTime();
        }
        dev.celli.enderutilities.util.ChunkLoading.getInstance().refresh(
                server.getServer(), target.dimension(),
                target.pos().getX() >> 4, target.pos().getZ() >> 4, -1, now);
    }

    private static ItemStack heldChunkLoadingItem(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof ItemEnderBag) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof ItemEnderBag ? off : ItemStack.EMPTY;
    }
}
