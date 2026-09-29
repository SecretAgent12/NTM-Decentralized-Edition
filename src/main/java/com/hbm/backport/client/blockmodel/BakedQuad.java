// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.hbm.backport.client.rendertype.ChunkSectionLayer;
import com.hbm.interfaces.injected.IQuadLightOrigin;
import java.util.Objects;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * 26.x net.minecraft.client.renderer.block.model.BakedQuad (NeoForge 26 shape): four positions in
 * block space, four packed UVs, a face direction, the material info, per-vertex normals and
 * per-vertex colours.
 *
 * <p>1.21.1 draws int[]-vertex quads; {@link #toVanilla()} bakes (and caches) the equivalent
 * {@link VanillaQuad}: vertex colour = baked colours (ARGB -> ABGR), UV2 = the light emission as
 * block light (NeoForge applies baked light as a per-component max), normal = baked normals,
 * tint/shade/AO from the material info. {@link #fromVanilla} goes the other way.
 */
public final class BakedQuad {

    public static final int VERTEX_COUNT = 4;
    public static final int FLAG_TRANSLUCENT = 1;
    public static final int FLAG_ANIMATED = 2;

    private final Vector3fc position0, position1, position2, position3;
    private final long packedUV0, packedUV1, packedUV2, packedUV3;
    private final Direction direction;
    private final MaterialInfo materialInfo;
    private final BakedNormals bakedNormals;
    private final BakedColors bakedColors;
    private volatile VanillaQuad vanilla;

    public BakedQuad(
            Vector3fc position0,
            Vector3fc position1,
            Vector3fc position2,
            Vector3fc position3,
            long packedUV0,
            long packedUV1,
            long packedUV2,
            long packedUV3,
            Direction direction,
            MaterialInfo materialInfo,
            BakedNormals bakedNormals,
            BakedColors bakedColors) {
        this.position0 = position0;
        this.position1 = position1;
        this.position2 = position2;
        this.position3 = position3;
        this.packedUV0 = packedUV0;
        this.packedUV1 = packedUV1;
        this.packedUV2 = packedUV2;
        this.packedUV3 = packedUV3;
        this.direction = direction;
        this.materialInfo = materialInfo;
        this.bakedNormals = bakedNormals == null ? BakedNormals.EMPTY : bakedNormals;
        this.bakedColors = bakedColors == null ? BakedColors.DEFAULT : bakedColors;
    }

    public BakedQuad(
            Vector3fc position0,
            Vector3fc position1,
            Vector3fc position2,
            Vector3fc position3,
            long packedUV0,
            long packedUV1,
            long packedUV2,
            long packedUV3,
            Direction direction,
            MaterialInfo materialInfo) {
        this(position0, position1, position2, position3, packedUV0, packedUV1, packedUV2, packedUV3,
                direction, materialInfo, BakedNormals.EMPTY, BakedColors.DEFAULT);
    }

    public Vector3fc position0() { return position0; }
    public Vector3fc position1() { return position1; }
    public Vector3fc position2() { return position2; }
    public Vector3fc position3() { return position3; }
    public long packedUV0() { return packedUV0; }
    public long packedUV1() { return packedUV1; }
    public long packedUV2() { return packedUV2; }
    public long packedUV3() { return packedUV3; }
    public Direction direction() { return direction; }
    public MaterialInfo materialInfo() { return materialInfo; }
    public BakedNormals bakedNormals() { return bakedNormals; }
    public BakedColors bakedColors() { return bakedColors; }

    public Vector3fc position(int vertex) {
        return switch (vertex) {
            case 0 -> position0;
            case 1 -> position1;
            case 2 -> position2;
            case 3 -> position3;
            default -> throw new IndexOutOfBoundsException(vertex);
        };
    }

    public long packedUV(int vertex) {
        return switch (vertex) {
            case 0 -> packedUV0;
            case 1 -> packedUV1;
            case 2 -> packedUV2;
            case 3 -> packedUV3;
            default -> throw new IndexOutOfBoundsException(vertex);
        };
    }

    public boolean isTinted() {
        return materialInfo.isTinted();
    }

    public TextureAtlasSprite sprite() {
        return materialInfo.sprite();
    }

    public BakedQuad withMaterialInfo(MaterialInfo info) {
        return new BakedQuad(position0, position1, position2, position3, packedUV0, packedUV1,
                packedUV2, packedUV3, direction, info, bakedNormals, bakedColors);
    }

    public BakedQuad withUVs(long uv0, long uv1, long uv2, long uv3) {
        return new BakedQuad(position0, position1, position2, position3, uv0, uv1, uv2, uv3,
                direction, materialInfo, bakedNormals, bakedColors);
    }

    public BakedQuad withColors(BakedColors colors) {
        return new BakedQuad(position0, position1, position2, position3, packedUV0, packedUV1,
                packedUV2, packedUV3, direction, materialInfo, bakedNormals, colors);
    }

    // ---------------------------------------------------------------- 1.21.1 conversion

    /** The 1.21.1 quad drawing this one (cached). */
    public VanillaQuad toVanilla() {
        VanillaQuad v = vanilla;
        if (v == null) vanilla = v = VanillaQuad.of(this);
        return v;
    }

    /** A 26.x quad for a 1.21.1 one, drawn in the given chunk layer. */
    public static BakedQuad fromVanilla(net.minecraft.client.renderer.block.model.BakedQuad quad, ChunkSectionLayer layer) {
        if (quad instanceof VanillaQuad own && own.source().materialInfo.layer == layer) return own.source();
        Transparency t = Layers.isTranslucent(layer) ? Transparency.TRANSLUCENT
                : layer == Layers.solid() ? Transparency.NONE : Transparency.TRANSPARENT;
        return VanillaQuad.decode(quad, layer, Layers.itemType(t));
    }

    /** A 26.x quad for a 1.21.1 item quad drawn with the given item render type. */
    public static BakedQuad fromVanillaItem(net.minecraft.client.renderer.block.model.BakedQuad quad, RenderType itemRenderType) {
        if (quad instanceof VanillaQuad own && own.source().materialInfo.itemRenderType == itemRenderType) return own.source();
        ChunkSectionLayer layer = itemRenderType == net.minecraft.client.renderer.Sheets.translucentItemSheet()
                || itemRenderType == RenderType.translucent()
                ? Layers.translucent() : Layers.layer(itemRenderType);
        return VanillaQuad.decode(quad, layer, itemRenderType);
    }

    /** A 26.x quad for a 1.21.1 one (layer from the quad's own origin when known, else cutout). */
    public static BakedQuad fromVanilla(net.minecraft.client.renderer.block.model.BakedQuad quad) {
        if (quad instanceof VanillaQuad own) return own.source();
        return VanillaQuad.decode(quad, Layers.cutout(), net.minecraft.client.renderer.Sheets.cutoutBlockSheet());
    }

    static Vector3f copy(Vector3fc v) {
        return new Vector3f(v);
    }

    /**
     * 26.x BakedQuad.MaterialInfo. Mutable only in the HBM light origin (26.x adds it with a
     * mixin, {@link IQuadLightOrigin}); equality includes it like the mixin does.
     */
    public static final class MaterialInfo implements IQuadLightOrigin {
        private final TextureAtlasSprite sprite;
        private final ChunkSectionLayer layer;
        private final RenderType itemRenderType;
        private final int tintIndex;
        private final boolean shade;
        private final int lightEmission;
        private final boolean ambientOcclusion;
        private int lightOrigin;
        private int flags = -1;

        public MaterialInfo(
                TextureAtlasSprite sprite,
                ChunkSectionLayer layer,
                RenderType itemRenderType,
                int tintIndex,
                boolean shade,
                int lightEmission,
                boolean ambientOcclusion) {
            this.sprite = sprite;
            this.layer = layer;
            this.itemRenderType = itemRenderType;
            this.tintIndex = tintIndex;
            this.shade = shade;
            this.lightEmission = lightEmission;
            this.ambientOcclusion = ambientOcclusion;
        }

        /** 26.x 6-component form (ambient occlusion on). */
        public MaterialInfo(
                TextureAtlasSprite sprite,
                ChunkSectionLayer layer,
                RenderType itemRenderType,
                int tintIndex,
                boolean shade,
                int lightEmission) {
            this(sprite, layer, itemRenderType, tintIndex, shade, lightEmission, true);
        }

        public static MaterialInfo of(
                Material.Baked material,
                Transparency transparency,
                int tintIndex,
                boolean shade,
                int lightEmission,
                boolean ambientOcclusion) {
            Transparency t = material.forceTranslucent() ? Transparency.TRANSLUCENT : transparency;
            return new MaterialInfo(material.sprite(), Layers.byTransparency(t), Layers.itemType(t),
                    tintIndex, shade, lightEmission, ambientOcclusion);
        }

        public static MaterialInfo of(
                Material.Baked material, Transparency transparency, int tintIndex, boolean shade, int lightEmission) {
            return of(material, transparency, tintIndex, shade, lightEmission, true);
        }

        public TextureAtlasSprite sprite() { return sprite; }
        public ChunkSectionLayer layer() { return layer; }
        public RenderType itemRenderType() { return itemRenderType; }
        public int tintIndex() { return tintIndex; }
        public boolean shade() { return shade; }
        public int lightEmission() { return lightEmission; }
        public boolean ambientOcclusion() { return ambientOcclusion; }

        public boolean isTinted() {
            return tintIndex != -1;
        }

        public int flags() {
            int f = flags;
            if (f < 0) {
                f = 0;
                if (Layers.isTranslucent(layer)) f |= FLAG_TRANSLUCENT;
                try {
                    if (sprite.contents().getUniqueFrames().limit(2).count() > 1) f |= FLAG_ANIMATED;
                } catch (RuntimeException ignored) {
                    // backport: contents closed
                }
                flags = f;
            }
            return f;
        }

        public MaterialInfo withTintIndex(int tint) {
            return copyOrigin(new MaterialInfo(sprite, layer, itemRenderType, tint, shade, lightEmission, ambientOcclusion));
        }

        public MaterialInfo withLayer(ChunkSectionLayer newLayer, RenderType newItemType) {
            return copyOrigin(new MaterialInfo(sprite, newLayer, newItemType, tintIndex, shade, lightEmission, ambientOcclusion));
        }

        private MaterialInfo copyOrigin(MaterialInfo m) {
            m.lightOrigin = lightOrigin;
            return m;
        }

        @Override
        public int hbm$lightOrigin() {
            return lightOrigin;
        }

        @Override
        public void hbm$setLightOrigin(int origin) {
            lightOrigin = origin;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MaterialInfo m
                    && m.sprite == sprite
                    && m.layer == layer
                    && Objects.equals(m.itemRenderType, itemRenderType)
                    && m.tintIndex == tintIndex
                    && m.shade == shade
                    && m.lightEmission == lightEmission
                    && m.ambientOcclusion == ambientOcclusion
                    && m.lightOrigin == lightOrigin;
        }

        @Override
        public int hashCode() {
            int h = System.identityHashCode(sprite);
            h = h * 31 + layer.hashCode();
            h = h * 31 + Objects.hashCode(itemRenderType);
            h = h * 31 + tintIndex;
            h = h * 31 + (shade ? 1 : 0);
            h = h * 31 + lightEmission;
            h = h * 31 + (ambientOcclusion ? 1 : 0);
            return 31 * h + lightOrigin;
        }
    }
}
