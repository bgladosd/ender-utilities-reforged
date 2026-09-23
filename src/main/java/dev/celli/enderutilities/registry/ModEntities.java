package dev.celli.enderutilities.registry;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.entity.EntityEnderPearlReusable;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EnderUtilities.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<EntityEnderPearlReusable>> ENDER_PEARL_REUSABLE =
            ENTITIES.register("enderpearlreusable", () -> EntityType.Builder
                    .<EntityEnderPearlReusable>of(EntityEnderPearlReusable::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("enderpearlreusable"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.celli.enderutilities.entity.EntityChair>> CHAIR =
            ENTITIES.register("chair", () -> EntityType.Builder
                    .<dev.celli.enderutilities.entity.EntityChair>of(dev.celli.enderutilities.entity.EntityChair::new, MobCategory.MISC)
                    .sized(0.5F, 0.75F)
                    .clientTrackingRange(4)
                    .updateInterval(Integer.MAX_VALUE)
                    .build("chair"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.celli.enderutilities.entity.EntityEnderArrow>> ENDER_ARROW =
            ENTITIES.register("enderarrow", () -> EntityType.Builder
                    .<dev.celli.enderutilities.entity.EntityEnderArrow>of(dev.celli.enderutilities.entity.EntityEnderArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("enderarrow"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.celli.enderutilities.entity.EntityEndermanFighter>> ENDERMAN_FIGHTER =
            ENTITIES.register("endermanfighter", () -> EntityType.Builder
                    .<dev.celli.enderutilities.entity.EntityEndermanFighter>of(
                            dev.celli.enderutilities.entity.EntityEndermanFighter::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.9F)
                    .clientTrackingRange(8)
                    .build("endermanfighter"));

    private ModEntities() {
    }
}
