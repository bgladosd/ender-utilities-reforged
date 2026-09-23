package dev.celli.enderutilities.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;

/**
 * Two-pass translucent ghosts: depth-only then color with no depth write.
 * 1.21 analog of orig GL dual-pass ({@code colorMask} false, then true +
 * {@code GL_LEQUAL}) without raw GL11. Opaque single-pass when
 * {@code buildersWandUseTranslucentGhostBlocks} is false.
 */
public final class GhostRenderTypes extends RenderType {
    /** Orig {@code buildersWandGhostBlockAlpha} default 0.7. */
    public static final int GHOST_ALPHA = 0xB3;

    public static final RenderType OPAQUE = create(
            "enderutilities_wand_ghost_opaque",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            2097152,
            true,
            false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_SOLID_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(true)
    );

    public static final RenderType DEPTH = create(
            "enderutilities_wand_ghost_depth",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            2097152,
            true,
            true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setWriteMaskState(DEPTH_WRITE)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .createCompositeState(true)
    );

    public static final RenderType COLOR = create(
            "enderutilities_wand_ghost_color",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            2097152,
            true,
            true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .createCompositeState(true)
    );

    private GhostRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }
}
