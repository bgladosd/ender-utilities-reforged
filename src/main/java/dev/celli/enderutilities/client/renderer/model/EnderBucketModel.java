package dev.celli.enderutilities.client.renderer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemEnderBucket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * NeoForge port of orig {@code ModelEnderBucket}: layered 32px body (normal vs
 * linked), inset insides, optional fill/drain/bind overlay, and fluid still
 * quads clipped to the two chambers.
 */
public class EnderBucketModel implements IUnbakedGeometry<EnderBucketModel> {
    static final List<Material> ATLAS_MATERIALS = allBucketMaterials();

    private static final Transformation INSIDE_Z = new Transformation(
            new Vector3f(), null, new Vector3f(1f, 1f, 0.95f), null);
    private static final Transformation MODE_Z = new Transformation(
            new Vector3f(0f, 0f, 0.0125f), null, new Vector3f(1f, 1f, 1f), null);

    private final boolean linked;
    private final String mode;
    @Nullable
    private final ResourceLocation fluidStill;
    private final float fill;

    public EnderBucketModel() {
        this(false, "none", null, 0f);
    }

    EnderBucketModel(boolean linked, String mode, @Nullable ResourceLocation fluidStill, float fill) {
        this.linked = linked;
        this.mode = mode;
        this.fluidStill = fluidStill;
        this.fill = fill;
    }

    EnderBucketModel withStackState(boolean linked, String mode, @Nullable ResourceLocation fluidStill, float fill) {
        return new EnderBucketModel(linked, mode, fluidStill, fill);
    }

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides
    ) {
        for (Material material : ATLAS_MATERIALS) {
            spriteGetter.apply(material);
        }
        ItemOverrides resolvedOverrides = overrides instanceof Handler handler
                ? handler
                : new Handler(this, context.getTransforms(), modelState);
        return bakeQuads(spriteGetter, modelState, context.getTransforms(), resolvedOverrides);
    }

    Baked bakeQuads(
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemTransforms transforms,
            ItemOverrides overrides
    ) {
        String prefix = linked ? "linked" : "normal";
        Material main = itemMaterial("enderbucket_32_" + prefix + "_main");
        Material insideTop = itemMaterial("enderbucket_32_" + prefix + "_insidetop");
        Material insideBottom = itemMaterial("enderbucket_32_" + prefix + "_insidebottom");
        Material modeTex = !"none".equals(mode) && !"null".equals(mode)
                ? itemMaterial("enderbucket_32_mode_" + mode)
                : null;

        List<BakedQuad> quads = new ArrayList<>();
        addLayer(quads, spriteGetter, modelState, main, false);
        addLayer(quads, spriteGetter, scaled(modelState, INSIDE_Z), insideTop, true);
        addLayer(quads, spriteGetter, scaled(modelState, INSIDE_Z), insideBottom, true);
        if (modeTex != null) {
            addLayer(quads, spriteGetter, scaled(modelState, MODE_Z), modeTex, true);
        }
        if (fluidStill != null && fill > 0f) {
            TextureAtlasSprite fluidSprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, fluidStill));
            addFluid(quads, fluidSprite, modelState, fill);
        }
        return new Baked(List.copyOf(quads), spriteGetter.apply(main), transforms, overrides);
    }

    private static void addLayer(
            List<BakedQuad> quads,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState state,
            Material material,
            boolean mask
    ) {
        TextureAtlasSprite sprite = spriteGetter.apply(material);
        if (sprite == null || sprite.contents().width() <= 0) {
            return;
        }
        var unbaked = mask
                ? UnbakedGeometryHelper.createUnbakedItemMaskElements(0, sprite)
                : UnbakedGeometryHelper.createUnbakedItemElements(0, sprite);
        quads.addAll(UnbakedGeometryHelper.bakeElements(unbaked, $ -> sprite, state));
    }

    /**
     * Orig {@code ModelEnderBucket} fluid quads, in 16-unit item pixels:
     * top chamber {@code x 4..12, y 3..7}; bottom {@code x 6.5..10, y 9..13}.
     * Those Y values are texture-down; vanilla model Y is up.
     */
    private static void addFluid(List<BakedQuad> quads, TextureAtlasSprite sprite, ModelState state, float fill) {
        float height = Mth.clamp(fill, 0f, 1f);
        if (height <= 0f || sprite == null) {
            return;
        }
        float yt = 7f - height * 4f;
        float yb = 13f - height * 4f;
        addFluidChamber(quads, sprite, state, 4f, yt, 12f, 7f);
        addFluidChamber(quads, sprite, state, 6.5f, yb, 10f, 13f);
    }

    private static void addFluidChamber(
            List<BakedQuad> quads,
            TextureAtlasSprite sprite,
            ModelState state,
            float x1,
            float yTexTop,
            float x2,
            float yTexBottom
    ) {
        if (yTexTop >= yTexBottom - 0.01f) {
            return;
        }
        float y1 = 16f - yTexBottom;
        float y2 = 16f - yTexTop;
        Map<Direction, BlockElementFace> faces = new HashMap<>();
        faces.put(Direction.NORTH, new BlockElementFace(null, 1, "fluid", new BlockFaceUV(null, 0)));
        faces.put(Direction.SOUTH, new BlockElementFace(null, 1, "fluid", new BlockFaceUV(null, 0)));
        BlockElement element = new BlockElement(
                new Vector3f(x1, y1, 0.469f * 16f),
                new Vector3f(x2, y2, 0.531f * 16f),
                faces,
                null,
                true
        );
        quads.addAll(UnbakedGeometryHelper.bakeElements(List.of(element), $ -> sprite, state));
    }

    private static ModelState scaled(ModelState state, Transformation extra) {
        return new SimpleModelState(state.getRotation().compose(extra), state.isUvLocked());
    }

    static Material itemMaterial(String name) {
        return new Material(
                TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "item/" + name)
        );
    }

    private static List<Material> allBucketMaterials() {
        List<Material> materials = new ArrayList<>();
        for (String body : List.of("normal", "linked")) {
            materials.add(itemMaterial("enderbucket_32_" + body + "_main"));
            materials.add(itemMaterial("enderbucket_32_" + body + "_insidetop"));
            materials.add(itemMaterial("enderbucket_32_" + body + "_insidebottom"));
        }
        for (String mode : List.of("fill", "drain", "bind")) {
            materials.add(itemMaterial("enderbucket_32_mode_" + mode));
        }
        return List.copyOf(materials);
    }

    static final class Baked implements BakedModel {
        private final List<BakedQuad> quads;
        private final TextureAtlasSprite particle;
        private final ItemTransforms transforms;
        private final ItemOverrides overrides;

        Baked(List<BakedQuad> quads, TextureAtlasSprite particle, ItemTransforms transforms, ItemOverrides overrides) {
            this.quads = quads;
            this.particle = particle;
            this.transforms = transforms;
            this.overrides = overrides;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return side == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public boolean isGui3d() {
            return false;
        }

        @Override
        public boolean usesBlockLight() {
            return false;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return particle;
        }

        @Override
        public ItemTransforms getTransforms() {
            return transforms;
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }

    private static final class Handler extends ItemOverrides {
        private final Map<String, BakedModel> cache = new HashMap<>();
        private final EnderBucketModel parent;
        private final ItemTransforms transforms;
        private final ModelState modelState;

        private Handler(EnderBucketModel parent, ItemTransforms transforms, ModelState modelState) {
            this.parent = parent;
            this.transforms = transforms;
            this.modelState = modelState;
        }

        @Override
        public BakedModel resolve(
                BakedModel originalModel,
                ItemStack stack,
                @Nullable ClientLevel level,
                @Nullable LivingEntity entity,
                int seed
        ) {
            if (!(stack.getItem() instanceof ItemEnderBucket bucket)) {
                return originalModel;
            }
            boolean linked = ItemEnderBucket.isLinked(stack);
            String mode = switch (bucket.mode(stack)) {
                case ItemEnderBucket.MODE_FILL -> "fill";
                case ItemEnderBucket.MODE_DRAIN -> "drain";
                case ItemEnderBucket.MODE_BIND -> "bind";
                default -> "none";
            };
            FluidStack fluid = bucket.getFluidCached(stack);
            ResourceLocation fluidStill = null;
            String fluidId = "empty";
            int amount = 0;
            int capacity = Math.max(1, bucket.getCapacityCached(stack, null));
            if (!fluid.isEmpty()) {
                amount = fluid.getAmount();
                fluidId = String.valueOf(BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
                IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
                fluidStill = ext.getStillTexture(fluid);
                if (fluidStill == null) {
                    fluidStill = ext.getStillTexture();
                }
            }
            int fillStep = Math.min(32, (int) ((amount / (float) capacity) * 32f));
            float fill = fillStep / 32f;
            String key = linked + "_" + mode + "_" + fluidId + "_" + fillStep;
            ResourceLocation still = fluidStill;
            return cache.computeIfAbsent(key, ignored -> parent
                    .withStackState(linked, mode, still, fill)
                    .bakeQuads(Handler::atlasSprite, modelState, transforms, this));
        }

        private static TextureAtlasSprite atlasSprite(Material material) {
            return Minecraft.getInstance().getModelManager().getAtlas(material.atlasLocation()).getSprite(material.texture());
        }
    }

    public static final class Loader implements IGeometryLoader<EnderBucketModel> {
        public static final Loader INSTANCE = new Loader();

        private Loader() {
        }

        @Override
        public EnderBucketModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
            return new EnderBucketModel();
        }
    }
}
