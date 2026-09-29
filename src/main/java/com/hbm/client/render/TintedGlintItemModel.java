// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.google.common.base.Suppliers;
import com.hbm.main.ResourceManager;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import com.hbm.backport.client.itemmodel.ItemModel;
import com.hbm.backport.client.itemmodel.ItemModelResolver;
import com.hbm.backport.client.itemmodel.ItemStackRenderState;
import com.hbm.backport.client.itemmodel.ModelRenderProperties;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.blockmodel.ResolvableModel;
import com.hbm.backport.client.blockmodel.BakedQuad;
import net.minecraft.resources.ResourceLocation;
import com.hbm.backport.ARGB;
import net.minecraft.util.ExtraCodecs;
import com.hbm.backport.client.itemmodel.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class TintedGlintItemModel implements ItemModel {

    private static final float GLINT_COLOR = 0.36F;
    private static final int GLINT_LAYERS = 2;
    private static final int TINT_INDEX = 0;

    private final com.hbm.backport.client.itemmodel.ItemQuads.Converted quads;
    private final Supplier<List<List<BakedQuad>>> glint;
    private final int tint;
    private final Supplier<Vector3fc[]> extents;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;

    private TintedGlintItemModel(
            com.hbm.backport.client.itemmodel.ItemQuads.Converted quads,
            Vector3fc color,
            ModelRenderProperties properties,
            Matrix4fc transformation) {
        this.quads = quads;
        this.properties = properties;
        this.transformation = transformation;
        this.tint =
                ARGB.colorFromFloat(
                        1F,
                        color.x() * GLINT_COLOR,
                        color.y() * GLINT_COLOR,
                        color.z() * GLINT_COLOR);
        this.glint =
                Suppliers.memoize(
                        () -> {
                            List<List<BakedQuad>> passes = new ArrayList<>(GLINT_LAYERS);
                            for (int layer = 0; layer < GLINT_LAYERS; layer++)
                                passes.add(glintPass(quads.quads(), layer));
                            return List.copyOf(passes);
                        });
        this.extents =
                quads.extents();
    }

    private static List<BakedQuad> glintPass(List<BakedQuad> quads, int layer) {
        RenderType type = WeaponRenderTypes.itemGlint(ResourceManager.glint_tex, layer);
        List<BakedQuad> pass = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            BakedQuad.MaterialInfo original = quad.materialInfo();
            // backport: same material with the glint render type, tint 0 (26.x: new MaterialInfo)
            BakedQuad.MaterialInfo material =
                    original.withLayer(original.layer(), type).withTintIndex(TINT_INDEX);
            pass.add(
                    quad.withMaterialInfo(material)
                            .withUVs(
                                    uv(quad.position(0)),
                                    uv(quad.position(1)),
                                    uv(quad.position(2)),
                                    uv(quad.position(3))));
        }
        return List.copyOf(pass);
    }

    private static long uv(Vector3fc position) {
        return com.hbm.backport.client.blockmodel.Uv.pack(position.x(), 1F - position.y());
    }

    // backport: 26.x ItemDisplayContext.ON_SHELF does not exist in 1.21.1
    private static boolean glints(ItemDisplayContext context) {
        return switch (context) {
            case NONE, GROUND, FIXED -> false;
            default -> true;
        };
    }

    @Override
    public void update(
            ItemStackRenderState output,
            ItemStack item,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed) {
        output.appendModelIdentityElement(this);
        boolean glinting = glints(displayContext);
        output.ensureCapacity(glinting ? 1 + GLINT_LAYERS : 1);

        layer(output, displayContext).prepareQuadList().addAll(quads.quads());
        if (quads.animated()) output.setAnimated();
        if (!glinting) return;

        for (List<BakedQuad> pass : glint.get()) {
            ItemStackRenderState.LayerRenderState layer = layer(output, displayContext);
            layer.tintLayers().add(tint);
            layer.prepareQuadList().addAll(pass);
        }
        output.setAnimated();
    }

    private ItemStackRenderState.LayerRenderState layer(
            ItemStackRenderState output, ItemDisplayContext context) {
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        layer.setExtents(extents);
        layer.setLocalTransform(transformation);
        properties.applyToLayer(layer, context);
        return layer;
    }

    public record Unbaked(ResourceLocation base, Vector3fc color) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                ResourceLocation.CODEC
                                                        .fieldOf("base")
                                                        .forGetter(Unbaked::base),
                                                com.hbm.backport.client.itemmodel.JomlCodecs.VECTOR3F
                                                        .fieldOf("color")
                                                        .forGetter(Unbaked::color))
                                        .apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            // backport: quads of the 1.21.1 baked model (26.x: ResolvedModel.bakeTopGeometry)
            net.minecraft.client.resources.model.BakedModel model = context.bakedModel(base);
            return new TintedGlintItemModel(
                    com.hbm.backport.client.itemmodel.ItemQuads.convertDefault(model),
                    color,
                    ModelRenderProperties.fromBakedModel(model),
                    transformation);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
