// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.blockmodel.BakedQuad;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.MatrixUtil;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Item quad drawing (what 26.x's item feature renderer does for {@code submitItem}) and the
 * conversion of 1.21.1 baked item models into 26.x-shaped quads.
 */
public final class ItemQuads {
    private ItemQuads() {}

    /**
     * Draws item quads the way 1.21.1 ItemRenderer does: per render type (26.x carries it per quad in
     * MaterialInfo), foil / compass foil, tint colors by tint index. {@code quads} may hold backport
     * (26.x-shaped) or vanilla 1.21.1 quads. {@code foil} is {@link ItemStackRenderState.FoilType}.
     */
    public static void render(
            MultiBufferSource buffers,
            PoseStack.Pose pose,
            ItemDisplayContext context,
            int light,
            int overlay,
            int[] tints,
            List<?> quads,
            Enum<?> foil) {
        if (quads.isEmpty()) return;
        String foilName = foil == null ? "NONE" : foil.name();
        RenderType current = null;
        VertexConsumer consumer = null;
        for (Object o : quads) {
            net.minecraft.client.renderer.block.model.BakedQuad vanilla;
            RenderType type;
            int tintIndex;
            if (o instanceof BakedQuad quad) {
                vanilla = quad.toVanilla();
                type = quad.materialInfo().itemRenderType();
                tintIndex = quad.materialInfo().tintIndex();
            } else {
                vanilla = (net.minecraft.client.renderer.block.model.BakedQuad) o;
                type = Sheets.translucentItemSheet();
                tintIndex = vanilla.getTintIndex();
            }
            if (type == null) type = Sheets.translucentItemSheet();
            if (type != current || consumer == null) {
                current = type;
                consumer = buffer(buffers, type, pose, context, foilName);
            }
            int color = tintIndex >= 0 && tintIndex < tints.length ? tints[tintIndex] : -1;
            consumer.putBulkData(
                    pose,
                    vanilla,
                    FastColor.ARGB32.red(color) / 255F,
                    FastColor.ARGB32.green(color) / 255F,
                    FastColor.ARGB32.blue(color) / 255F,
                    FastColor.ARGB32.alpha(color) / 255F,
                    light,
                    overlay,
                    true);
        }
    }

    private static VertexConsumer buffer(
            MultiBufferSource buffers,
            RenderType type,
            PoseStack.Pose pose,
            ItemDisplayContext context,
            String foil) {
        switch (foil) {
            case "SPECIAL" -> {
                // port of 1.21.1 ItemRenderer.render's compass/clock foil branch
                PoseStack.Pose copy = pose.copy();
                if (context == ItemDisplayContext.GUI) MatrixUtil.mulComponentWise(copy.pose(), 0.5F);
                else if (context.firstPerson()) MatrixUtil.mulComponentWise(copy.pose(), 0.75F);
                return ItemRenderer.getCompassFoilBuffer(buffers, type, copy);
            }
            case "STANDARD" -> {
                return ItemRenderer.getFoilBufferDirect(buffers, type, true, true);
            }
            default -> {
                return buffers.getBuffer(type);
            }
        }
    }

    // ---- 1.21.1 baked model -> 26.x-shaped item quads ---------------------------------------

    private static final Map<BakedModel, Map<RenderType, Converted>> CACHE = new IdentityHashMap<>();

    /** Quads of one render pass of a 1.21.1 item model, converted once per resource reload. */
    public record Converted(
            List<BakedQuad> quads,
            int maxTintIndex,
            boolean animated,
            java.util.function.Supplier<org.joml.Vector3fc[]> extents) {
        Converted(List<BakedQuad> quads, int maxTintIndex, boolean animated) {
            this(
                    quads,
                    maxTintIndex,
                    animated,
                    com.google.common.base.Suppliers.memoize(
                            () -> CuboidItemModelWrapper.computeExtents(quads)));
        }
    }

    public static synchronized Converted convert(BakedModel model, RenderType type) {
        return CACHE.computeIfAbsent(model, m -> new IdentityHashMap<>())
                .computeIfAbsent(type, t -> doConvert(model, t));
    }

    /**
     * The quads of a 1.21.1 model for a non-block item (the model's own render_type, else NeoForge's
     * item fallback) — what 26.x bakeTopGeometry yields for an item model.
     */
    public static Converted convertDefault(BakedModel model) {
        List<RenderType> types =
                model.getRenderTypes(net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.client.Minecraft.useShaderTransparency());
        return convert(model, types.isEmpty() ? Sheets.translucentItemSheet() : types.get(0));
    }

    public static synchronized void clearCache() {
        CACHE.clear();
    }

    private static Converted doConvert(BakedModel model, RenderType type) {
        RandomSource random = RandomSource.create();
        List<BakedQuad> out = new ArrayList<>();
        int maxTint = -1;
        boolean animated = false;
        Direction[] dirs = Direction.values();
        for (int i = 0; i <= dirs.length; i++) {
            Direction dir = i < dirs.length ? dirs[i] : null;
            random.setSeed(42L);
            for (net.minecraft.client.renderer.block.model.BakedQuad q :
                    model.getQuads(null, dir, random, ModelData.EMPTY, type)) {
                out.add(BakedQuad.fromVanillaItem(q, type));
                maxTint = Math.max(maxTint, q.getTintIndex());
                if (!animated && q.getSprite() != null
                        && q.getSprite().contents().getUniqueFrames().count() > 1) animated = true;
            }
        }
        return new Converted(List.copyOf(out), maxTint, animated);
    }
}
