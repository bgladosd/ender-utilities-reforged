package dev.celli.enderutilities.item.tool;

import java.util.List;

import dev.celli.enderutilities.item.ItemEnderUtilities;
import dev.celli.enderutilities.item.module.IAnvilRepairable;
import dev.celli.enderutilities.item.module.IKeyBound;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Orig {@code ItemVoidPickaxe} extends {@code ItemEnderUtilities}, not {@code ItemEnderTool}.
 * No modules, charge, location bind, area mining, or drop-routing.
 */
public class ItemVoidPickaxe extends ItemEnderUtilities implements IKeyBound, IAnvilRepairable {
    public static final int MAX_USES = 3120;
    public static final float EFFICIENCY = 6.0F;
    public static final float FAST_EFFICIENCY = 24.0F;
    public static final float FAST_EFFICIENCY_V = 124.0F;
    public static final float BROKEN_SPEED = 0.1F;
    public static final double ATTACK_DAMAGE = 6.0;
    public static final double BROKEN_ATTACK_DAMAGE = 0.5;
    public static final double ATTACK_SPEED = -2.7;
    public static final int ENCHANTABILITY = 15;

    public ItemVoidPickaxe(Properties properties) {
        super(properties.stacksTo(1).durability(MAX_USES));
    }

    @Override
    public int getEnchantmentValue() {
        return ENCHANTABILITY;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    public boolean isFastMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_MODE, 0) != 0;
    }

    public boolean isToolBroken(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return attributes(ATTACK_DAMAGE);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return attributes(isToolBroken(stack) ? BROKEN_ATTACK_DAMAGE : ATTACK_DAMAGE);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (isToolBroken(stack)) {
            return BROKEN_SPEED;
        }
        float efficiency = EFFICIENCY;
        if (isFastMode(stack)) {
            efficiency = enchantmentLevel(stack, Enchantments.EFFICIENCY) >= 5 ? FAST_EFFICIENCY_V : FAST_EFFICIENCY;
        }
        if (isToolEffective(state) || isCorrectToolForDrops(stack, state)) {
            return efficiency;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return !isToolBroken(stack) && !state.is(Blocks.BARRIER);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (isToolBroken(stack)) {
            return false;
        }
        return itemAbility == ItemAbilities.PICKAXE_DIG
                || itemAbility == ItemAbilities.AXE_DIG
                || itemAbility == ItemAbilities.SHOVEL_DIG;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (!level.isClientSide && miningEntity instanceof Player player && !player.getAbilities().instabuild) {
            addToolDamage(stack, level, pos, state, miningEntity);
        }
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        addToolDamage(stack, 2, attacker);
        return true;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.ENDER_ALLOY_ADVANCED.get());
    }

    @Override
    public boolean isRepairItem(ItemStack stackTool, ItemStack stackMaterial) {
        return isValidRepairItem(stackTool, stackMaterial);
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (key == HotKeys.KEYBIND_ID_TOGGLE_MODE) {
            stack.set(ModDataComponents.ITEM_MODE, isFastMode(stack) ? 0 : 1);
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        String modeKey = isFastMode(stack)
                ? "enderutilities.tooltip.item.fast"
                : "enderutilities.tooltip.item.normal";
        tooltip.add(Component.translatable("enderutilities.tooltip.item.mode")
                .append(": ")
                .append(Component.translatable(modeKey).withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.GRAY));
    }

    public void handleBreakEvent(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return;
        }
        voidBlock(level, pos);
    }

    @Override
    public boolean repairItem(ItemStack stack, int amount) {
        if (amount == -1) {
            amount = MAX_USES;
        }
        int damage = Math.max(stack.getDamageValue() - amount, 0);
        boolean repaired = damage != stack.getDamageValue();
        stack.setDamageValue(damage);
        return repaired;
    }

    static void voidBlock(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) != null) {
            level.removeBlockEntity(pos);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private boolean addToolDamage(ItemStack stack, Level level, BlockPos pos, BlockState state, LivingEntity living) {
        if (isToolBroken(stack) || state.getDestroySpeed(level, pos) <= 0.0F) {
            return false;
        }
        int damage = isFastMode(stack) ? 2 : 1;
        addToolDamage(stack, damage, living);
        return true;
    }

    private void addToolDamage(ItemStack stack, int amount, LivingEntity living) {
        if (isToolBroken(stack)) {
            return;
        }
        if (amount > 0 && enchantmentLevel(stack, Enchantments.UNBREAKING) > 0) {
            int negated = 0;
            for (int i = 0; i < amount; i++) {
                if (living.getRandom().nextInt(amount + 1) > 0) {
                    negated++;
                }
            }
            amount -= negated;
            if (amount <= 0) {
                return;
            }
        }
        int damage = Math.min(stack.getDamageValue() + amount, MAX_USES);
        if (!living.level().isClientSide) {
            stack.setDamageValue(damage);
        }
        if (damage == MAX_USES) {
            living.onEquippedItemBroken(this, EquipmentSlot.MAINHAND);
        }
    }

    private static boolean isToolEffective(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    private static int enchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchants.keySet()) {
            if (holder.is(enchantment)) {
                return enchants.getLevel(holder);
            }
        }
        return 0;
    }

    private static ItemAttributeModifiers attributes(double attackDamage) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDamage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }
}
