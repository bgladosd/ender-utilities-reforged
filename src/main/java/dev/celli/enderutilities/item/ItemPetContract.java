package dev.celli.enderutilities.item;

import java.util.List;
import java.util.Optional;

import dev.celli.enderutilities.data.PetContractData;
import dev.celli.enderutilities.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ItemPetContract extends ItemEnderUtilities {
    public ItemPetContract(Properties properties) {
        super(properties);
    }

    public static boolean isSigned(ItemStack stack) {
        return stack.has(ModDataComponents.PET_CONTRACT);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return isSigned(stack) ? "item.enderutilities.pet_contract_signed" : "item.enderutilities.pet_contract_blank";
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        if (!isSigned(stack)) {
            return name;
        }
        return name.copy().append(" - ").append(Component.literal(targetDisplay(stack)));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof TamableAnimal tameable)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            PetContractData data = stack.get(ModDataComponents.PET_CONTRACT);
            if (data != null) {
                if (tameable.getOwnerUUID() != null
                        && tameable.getOwnerUUID().equals(data.owner())
                        && target.getUUID().equals(data.subject())
                        && !player.getUUID().equals(data.owner())) {
                    tameable.tame(player);
                    player.level().broadcastEntityEvent(target, (byte) 7);
                    stack.shrink(1);
                }
            } else if (tameable.isTame() && tameable.isOwnedBy(player)) {
                ItemStack signed = stack.copyWithCount(1);
                signed.set(ModDataComponents.PET_CONTRACT, new PetContractData(
                        player.getUUID(),
                        target.getUUID(),
                        BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString(),
                        target.hasCustomName() ? Optional.of(target.getCustomName().getString()) : Optional.empty(),
                        target.getHealth()
                ));
                if (!player.addItem(signed)) {
                    player.drop(signed, false);
                }
                stack.shrink(1);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.PLAYERS, 0.5F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!isSigned(stack)) {
            super.appendHoverText(stack, context, tooltip, flag);
        }
    }

    private static String targetDisplay(ItemStack stack) {
        PetContractData data = stack.get(ModDataComponents.PET_CONTRACT);
        if (data == null) {
            return "";
        }
        String id = data.entityId();
        String translated = Component.translatable("entity." + id.replace(':', '.')).getString();
        String name = data.customName().orElse("");
        if (name.isEmpty()) {
            return translated + String.format(" (%.1f HP)", data.health());
        }
        return ChatFormatting.WHITE.toString() + ChatFormatting.ITALIC + name + ChatFormatting.RESET
                + ChatFormatting.WHITE + " (" + translated + ")"
                + String.format(" (%.1f HP)", data.health());
    }
}
