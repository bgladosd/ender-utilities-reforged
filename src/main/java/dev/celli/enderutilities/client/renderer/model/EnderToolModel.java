package dev.celli.enderutilities.client.renderer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemEnderCore;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.item.tool.ItemEnderSword;
import dev.celli.enderutilities.item.tool.ItemEnderTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;

/**
 * NeoForge port of orig {@code ModelEnderTools}: one baked quad list (rod + head +
 * optional core/capacitor/link-crystal overlays). Module layers use the orig
 * {@code tr_*} TRSR so the small icons sit on the handle in front of the rod.
 * GUI and first-person both go through {@link ItemOverrides#resolve} with
 * {@code entity} possibly null.
 */
public class EnderToolModel implements IUnbakedGeometry<EnderToolModel> {
    private static final Map<String, Float> DEFAULT_MODULE_TRANSFORMS = origModuleTransforms();
    static final List<Material> ATLAS_MATERIALS = allToolMaterials();

    private final String toolClass;
    private final boolean powered;
    private final boolean creative;
    private final boolean broken;
    private final int mode;
    private final int core;
    private final int capacitor;
    private final int linkCrystal;
    private final Map<String, Float> moduleTransforms;

    public EnderToolModel(
            String toolClass,
            boolean powered,
            boolean creative,
            boolean broken,
            int mode,
            int core,
            int capacitor,
            int linkCrystal,
            Map<String, Float> moduleTransforms
    ) {
        this.toolClass = toolClass;
        this.powered = powered;
        this.creative = creative;
        this.broken = broken;
        this.mode = mode;
        this.core = core;
        this.capacitor = capacitor;
        this.linkCrystal = linkCrystal;
        this.moduleTransforms = moduleTransforms;
    }

    EnderToolModel withStackState(
            String toolClass,
            boolean powered,
            boolean creative,
            boolean broken,
            int mode,
            int core,
            int capacitor,
            int linkCrystal
    ) {
        return new EnderToolModel(toolClass, powered, creative, broken, mode, core, capacitor, linkCrystal, moduleTransforms);
    }

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides
    ) {
        // Touch every overlay sprite during the initial bake so the block atlas
        // has already resolved them if the JSON/atlas sources listed them.
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
        String headName = toolClass + "_head_" + (broken ? "broken_" : "") + (powered ? "glow_" : "normal_") + mode;
        Material rod = itemMaterial("endertool_" + toolClass + "_rod" + (usesCreativeRod() ? "_2" : ""));
        Material head = itemMaterial("endertool_" + headName);
        Material coreTex = core >= 1 && core <= 3 ? itemMaterial("endertool_module_core_" + core) : null;
        Material capTex = capacitor >= 1 && capacitor <= 4 ? itemMaterial("endertool_module_capacitor_" + capacitor) : null;
        Material crystalTex = linkCrystal >= 1 && linkCrystal <= 2 ? itemMaterial("endertool_module_linkcrystal_" + linkCrystal) : null;

        // Orig bake order: rod, head, then module overlays so later quads draw on top.
        List<BakedQuad> quads = new ArrayList<>();
        addLayer(quads, spriteGetter, modelState, rod, false);
        addLayer(quads, spriteGetter, modelState, head, false);
        if (coreTex != null) {
            addLayer(quads, spriteGetter, transformedState(modelState, "co"), coreTex, true);
        }
        if (capTex != null) {
            addLayer(quads, spriteGetter, transformedState(modelState, "ca"), capTex, true);
        }
        if (crystalTex != null) {
            addLayer(quads, spriteGetter, transformedState(modelState, "lc"), crystalTex, true);
        }
        return new Baked(List.copyOf(quads), spriteGetter.apply(rod), transforms, overrides);
    }

    private boolean usesCreativeRod() {
        return creative && !"sword".equals(toolClass) && !"hoe".equals(toolClass);
    }

    private ModelState transformedState(ModelState state, String module) {
        ModuleTransforms mt = new ModuleTransforms(moduleTransforms, toolClass, module);
        // Orig 1.12 ItemLayerModel applied IModelState around the origin, so it
        // wrapped the handle TRSR in blockCenterToCorner(). 1.21 FaceBakery
        // already does `matrix * (vec - 0.5) + 0.5`, so that extra conversion
        // double-shifts Z and parks the icons behind the blade.
        Transformation tr = new Transformation(new Vector3f(mt.tx, mt.ty, mt.tz), null, new Vector3f(mt.sx, mt.sy, mt.sz), null);
        return new SimpleModelState(state.getRotation().compose(tr), state.isUvLocked());
    }

    private static void addLayer(
            List<BakedQuad> quads,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState state,
            Material material,
            boolean moduleOverlay
    ) {
        TextureAtlasSprite sprite = spriteGetter.apply(material);
        if (sprite == null || sprite.contents().width() <= 0) {
            return;
        }
        // Module textures are small handle icons. A full 16x16 item layer (north/south
        // covering the whole sprite) looks like a giant square through the blade,
        // and 1.21 item rendering no longer alpha-tests those transparent pixels.
        var unbaked = moduleOverlay
                ? UnbakedGeometryHelper.createUnbakedItemMaskElements(0, sprite)
                : UnbakedGeometryHelper.createUnbakedItemElements(0, sprite);
        quads.addAll(UnbakedGeometryHelper.bakeElements(unbaked, $ -> sprite, state));
    }

    static Material itemMaterial(String name) {
        return new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "item/" + name));
    }

    private static List<Material> allToolMaterials() {
        List<Material> materials = new ArrayList<>();
        for (String tool : List.of("sword", "pickaxe", "axe", "shovel", "hoe")) {
            materials.add(itemMaterial("endertool_" + tool + "_rod"));
            if (!"sword".equals(tool) && !"hoe".equals(tool)) {
                materials.add(itemMaterial("endertool_" + tool + "_rod_2"));
            }
        }
        for (int i = 1; i <= 3; i++) {
            materials.add(itemMaterial("endertool_module_core_" + i));
        }
        for (int i = 1; i <= 4; i++) {
            materials.add(itemMaterial("endertool_module_capacitor_" + i));
        }
        materials.add(itemMaterial("endertool_module_linkcrystal_1"));
        materials.add(itemMaterial("endertool_module_linkcrystal_2"));
        return List.copyOf(materials);
    }

    private static Map<String, Float> origModuleTransforms() {
        Map<String, Float> map = new HashMap<>();
        putTransform(map, "p", "co", 1.02f, 1.02f, 2.0f, 0.625f, -0.1875f, 0f);
        putTransform(map, "a", "co", 1.02f, 1.02f, 2.0f, 0.5625f, -0.25f, 0f);
        putTransform(map, "h", "co", 1.02f, 1.02f, 2.0f, 0.625f, -0.1875f, 0f);
        putTransform(map, "s", "co", 1.02f, 1.02f, 2.0f, 0.5625f, -0.25f, 0f);
        putTransform(map, "w", "co", 1.02f, 1.02f, 2.0f, 0.3125f, -0.4375f, 0f);
        putTransform(map, "p", "ca", 1.02f, 1.02f, 1.7f, 0.5f, -0.3125f, 0f);
        putTransform(map, "a", "ca", 1.02f, 1.02f, 1.7f, 0.4375f, -0.375f, 0f);
        putTransform(map, "h", "ca", 1.02f, 1.02f, 1.7f, 0.5f, -0.3125f, 0f);
        putTransform(map, "s", "ca", 1.02f, 1.02f, 1.7f, 0.4375f, -0.375f, 0f);
        putTransform(map, "w", "ca", 1.02f, 1.02f, 1.7f, 0.1875f, -0.5625f, 0f);
        putTransform(map, "p", "lc", 1.02f, 1.02f, 1.5f, 0.8125f, -0.125f, 0f);
        putTransform(map, "a", "lc", 1.02f, 1.02f, 1.5f, 0.75f, -0.1875f, 0f);
        putTransform(map, "h", "lc", 1.02f, 1.02f, 1.5f, 0.8125f, -0.125f, 0f);
        putTransform(map, "s", "lc", 1.02f, 1.02f, 1.5f, 0.125f, -0.8125f, 0f);
        putTransform(map, "w", "lc", 1.02f, 1.02f, 1.5f, 0f, -0.875f, 0f);
        return Map.copyOf(map);
    }

    private static void putTransform(Map<String, Float> map, String tool, String module, float sx, float sy, float sz, float tx, float ty, float tz) {
        map.put("tr_sx_" + tool + "_" + module, sx);
        map.put("tr_sy_" + tool + "_" + module, sy);
        map.put("tr_sz_" + tool + "_" + module, sz);
        map.put("tr_tx_" + tool + "_" + module, tx);
        map.put("tr_ty_" + tool + "_" + module, ty);
        map.put("tr_tz_" + tool + "_" + module, tz);
    }

    private static final class ModuleTransforms {
        final float tx;
        final float ty;
        final float tz;
        final float sx;
        final float sy;
        final float sz;

        private ModuleTransforms(Map<String, Float> transforms, String tool, String module) {
            String id = "sword".equals(tool) ? "w" : tool.substring(0, 1);
            this.tx = value(transforms, "tr_tx_" + id + "_" + module, 0f);
            this.ty = value(transforms, "tr_ty_" + id + "_" + module, 0f);
            this.tz = value(transforms, "tr_tz_" + id + "_" + module, 0f);
            this.sx = value(transforms, "tr_sx_" + id + "_" + module, 1.02f);
            this.sy = value(transforms, "tr_sy_" + id + "_" + module, 1.02f);
            this.sz = value(transforms, "tr_sz_" + id + "_" + module, 1.6f);
        }

        private static float value(Map<String, Float> transforms, String key, float fallback) {
            Float value = transforms.get(key);
            return value != null ? value : fallback;
        }
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
        private final EnderToolModel parent;
        private final ItemTransforms transforms;
        private final ModelState modelState;

        private Handler(EnderToolModel parent, ItemTransforms transforms, ModelState modelState) {
            this.parent = parent;
            this.transforms = transforms;
            this.modelState = modelState;
        }

        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            String toolClass;
            boolean broken;
            boolean powered;
            boolean creative;
            int mode;
            int core = overlayIndex(stack, ModuleType.ENDERCORE, 3);
            int capacitor = overlayIndex(stack, ModuleType.ENDERCAPACITOR, 4);
            int linkCrystal = overlayIndex(stack, ModuleType.LINKCRYSTAL, 2);

            if (stack.getItem() instanceof ItemEnderTool tool) {
                toolClass = toolClass(tool);
                broken = isBroken(stack);
                powered = tool.isFast(stack);
                creative = ModularHelper.getInstalledModuleCount(stack, ModuleType.CREATIVE_BREAKING) > 0;
                mode = tool.dropsMode(stack) + 1;
            } else if (stack.getItem() instanceof ItemEnderSword sword) {
                toolClass = "sword";
                broken = isBroken(stack);
                powered = false;
                creative = false;
                mode = sword.dropsMode(stack) + 1;
            } else {
                return originalModel;
            }

            String key = toolClass + broken + powered + creative + mode + core + capacitor + linkCrystal;
            return cache.computeIfAbsent(key, ignored -> parent
                    .withStackState(toolClass, powered, creative, broken, mode, core, capacitor, linkCrystal)
                    .bakeQuads(Handler::atlasSprite, modelState, transforms, this));
        }

        private static TextureAtlasSprite atlasSprite(Material material) {
            return Minecraft.getInstance().getModelManager().getAtlas(material.atlasLocation()).getSprite(material.texture());
        }

        private static int overlayIndex(ItemStack stack, ModuleType type, int maxTexture) {
            ItemStack module = ModularHelper.getSelectedModuleStack(stack, type);
            if (module.isEmpty() || !(module.getItem() instanceof IModule item)) {
                return 0;
            }
            if (item instanceof ItemEnderCore core && !core.isActive()) {
                return 0;
            }
            int index = item.getModuleTier(module) + 1;
            return index >= 1 && index <= maxTexture ? index : 0;
        }

        private static String toolClass(ItemEnderTool tool) {
            return switch (tool.kind()) {
                case PICKAXE -> "pickaxe";
                case AXE -> "axe";
                case SHOVEL -> "shovel";
                case HOE -> "hoe";
            };
        }

        private static boolean isBroken(ItemStack stack) {
            return stack.getDamageValue() >= stack.getMaxDamage() - 1;
        }
    }

    public static final class Loader implements IGeometryLoader<EnderToolModel> {
        public static final Loader INSTANCE = new Loader();

        private Loader() {
        }

        @Override
        public EnderToolModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
            String toolClass = GsonHelper.getAsString(jsonObject, "toolClass");
            Map<String, Float> transforms = new HashMap<>(DEFAULT_MODULE_TRANSFORMS);
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("tr_") && entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    transforms.put(key, entry.getValue().getAsFloat());
                }
            }
            return new EnderToolModel(toolClass, false, false, false, 1, 0, 0, 0, Map.copyOf(transforms));
        }
    }
}
