// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Bakes item models into immutable {@link BakedMesh}es, bucketed by layer and atlas.
 * <p>backport: CrankShaft's interface is implemented per loader over 26.x item render states and block state models;
 * this is a 1.21.1 implementation over BakedModel, following ItemRenderer#render (NeoForge render passes and render
 * types, display transform, tint, baked light). Only the item entry points NTM uses are ported (bufferItem,
 * bufferItemInVisualFrame); items with a custom renderer (BEWLR) bake to {@code null} like 26.x special renderers.
 */
public final class BakedModelBufferer {
    public static final BakedModelBufferer INSTANCE = new BakedModelBufferer();
    private static final Direction[] DIRECTIONS = Direction.values();

    private BakedModelBufferer() {
    }

    /** backport: stands in for 26.x ChunkSectionLayer as the bucket layer (ModelUtil.getItemMaterial takes either). */
    public enum Layer {
        SOLID, CUTOUT, TRANSLUCENT
    }

    /**
     * Bakes an item stack's resolved geometry into immutable meshes in the item's display space.
     *
     * @param owner the holder (26.x ItemOwner); a LivingEntity is used for model override resolution.
     */
    public @Nullable ItemMeshes bufferItem(ItemStack stack, ItemDisplayContext displayContext, @Nullable Object owner,
                                           int seed) {
        return buffer(stack, displayContext, owner, seed, true);
    }

    /**
     * {@link #bufferItem} in the frame ItemStackVisual.beginFrame's pose places: the display transform left to that
     * pose.
     */
    public @Nullable ItemMeshes bufferItemInVisualFrame(ItemStack stack, ItemDisplayContext displayContext,
                                                        @Nullable Object owner, int seed) {
        return buffer(stack, displayContext, owner, seed, false);
    }

    private @Nullable ItemMeshes buffer(ItemStack stack, ItemDisplayContext displayContext, @Nullable Object owner,
                                        int seed, boolean displayTransform) {
        if (stack.isEmpty()) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        LivingEntity living = owner instanceof LivingEntity entity ? entity : null;
        Level level = owner instanceof Entity entity ? entity.level() : minecraft.level;
        BakedModel model = itemRenderer.getModel(stack, level, living, seed);
        BakedModel base = itemRenderer.getItemModelShaper().getItemModel(stack);
        // backport: 26.x knows whether the item model reads its holder; 1.21.1 overrides are opaque, so an item with
        // overrides counts as stack-determined only if resolving without level/holder gives the same model.
        boolean stackDetermined = base.getOverrides() == ItemOverrides.EMPTY
                || base.getOverrides().resolve(base, stack, null, null, seed) == model;
        boolean leftHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        PoseStack poseStack = new PoseStack();
        model = net.neoforged.neoforge.client.ClientHooks.handleCameraTransforms(
                displayTransform ? poseStack : new PoseStack(), model, displayContext, leftHand);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        if (model.isCustomRenderer()) {
            return null;
        }
        boolean fabulous = true;
        if (displayContext != ItemDisplayContext.GUI && !displayContext.firstPerson()
                && stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
            var block = blockItem.getBlock();
            fabulous = !(block instanceof net.minecraft.world.level.block.HalfTransparentBlock)
                    && !(block instanceof net.minecraft.world.level.block.StainedGlassPaneBlock);
        }
        Map<ItemMeshKey, Builder> builders = new EnumMap<>(ItemMeshKey.class);
        RandomSource random = RandomSource.create();
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        for (BakedModel pass : model.getRenderPasses(stack, fabulous)) {
            for (RenderType renderType : pass.getRenderTypes(stack, fabulous)) {
                ItemMeshKey key = ItemMeshKey.of(isTranslucent(renderType), true);
                Builder builder = builders.computeIfAbsent(key, k -> new Builder());
                // backport: the quad type is left to inference (var) -- the backport redirects the 1.21.1 BakedQuad
                // name to the 26.x-shaped shim, but getQuads returns vanilla quads.
                for (int side = 0; side <= DIRECTIONS.length; side++) {
                    random.setSeed(42L);
                    for (var quad : pass.getQuads(null, side < DIRECTIONS.length ? DIRECTIONS[side] : null, random)) {
                        int tint = quad.isTinted()
                                ? minecraft.getItemColors().getColor(stack, quad.getTintIndex()) : -1;
                        builder.add(quad.getVertices(), tint, quad.getDirection(), pose, normal);
                    }
                }
            }
        }
        Map<ItemMeshKey, BakedMesh> meshes = new EnumMap<>(ItemMeshKey.class);
        float minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (var entry : builders.entrySet()) {
            Builder b = entry.getValue();
            if (b.vertices == 0) continue;
            meshes.put(entry.getKey(), b.build(stack));
            minY = Math.min(minY, b.minY);
            minZ = Math.min(minZ, b.minZ);
            maxZ = Math.max(maxZ, b.maxZ);
        }
        if (meshes.isEmpty()) {
            minY = 0;
            minZ = maxZ = 0;
        }
        return new ItemMeshes(meshes, stack.hasFoil(), minY, maxZ - minZ, stackDetermined, !stackDetermined, model);
    }

    // backport: 1.21.1 item render types are opaque objects; translucency is read from the vanilla sheet / chunk types
    // and, for others, the render type name (unverified for modded render types).
    private static boolean isTranslucent(RenderType renderType) {
        return renderType == RenderType.translucent()
                || renderType == net.minecraft.client.renderer.Sheets.translucentItemSheet()
                || renderType == net.minecraft.client.renderer.Sheets.translucentCullBlockSheet()
                || renderType.toString().contains("translucent");
    }

    private static final class Builder {
        private final List<float[]> positions = new ArrayList<>();
        private final List<float[]> uvs = new ArrayList<>();
        private final List<float[]> normals = new ArrayList<>();
        private final List<int[]> colors = new ArrayList<>();
        private final List<int[]> lights = new ArrayList<>();
        private int vertices;
        private float minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        private final Vector3f scratch = new Vector3f();

        void add(int[] data, int tint, Direction direction, Matrix4f pose, Matrix3f normalMatrix) {
            {
                int stride = data.length / 4;
                float[] p = new float[12], t = new float[8], n = new float[12];
                int[] c = new int[4], l = new int[4];
                for (int v = 0; v < 4; v++) {
                    int o = v * stride;
                    pose.transformPosition(Float.intBitsToFloat(data[o]), Float.intBitsToFloat(data[o + 1]),
                            Float.intBitsToFloat(data[o + 2]), scratch);
                    p[v * 3] = scratch.x;
                    p[v * 3 + 1] = scratch.y;
                    p[v * 3 + 2] = scratch.z;
                    minY = Math.min(minY, scratch.y);
                    minZ = Math.min(minZ, scratch.z);
                    maxZ = Math.max(maxZ, scratch.z);
                    int abgr = data[o + 3];
                    int r = (abgr & 0xFF) * (tint >> 16 & 0xFF) / 255;
                    int g = (abgr >> 8 & 0xFF) * (tint >> 8 & 0xFF) / 255;
                    int b = (abgr >> 16 & 0xFF) * (tint & 0xFF) / 255;
                    int a = (abgr >>> 24) * (tint >>> 24) / 255;
                    c[v] = a << 24 | r << 16 | g << 8 | b;
                    t[v * 2] = Float.intBitsToFloat(data[o + 4]);
                    t[v * 2 + 1] = Float.intBitsToFloat(data[o + 5]);
                    l[v] = data[o + 6];
                    int packed = data[o + 7];
                    float nx = (byte) (packed & 0xFF) / 127f, ny = (byte) (packed >> 8 & 0xFF) / 127f,
                            nz = (byte) (packed >> 16 & 0xFF) / 127f;
                    if (nx == 0 && ny == 0 && nz == 0) {
                        var dir = direction.getNormal();
                        nx = dir.getX();
                        ny = dir.getY();
                        nz = dir.getZ();
                    }
                    normalMatrix.transform(nx, ny, nz, scratch).normalize();
                    n[v * 3] = scratch.x;
                    n[v * 3 + 1] = scratch.y;
                    n[v * 3 + 2] = scratch.z;
                }
                positions.add(p);
                uvs.add(t);
                normals.add(n);
                colors.add(c);
                lights.add(l);
                vertices += 4;
            }
        }

        BakedMesh build(ItemStack stack) {
            float[] p = new float[vertices * 3], t = new float[vertices * 2], n = new float[vertices * 3];
            int[] c = new int[vertices], o = new int[vertices], l = new int[vertices];
            for (int q = 0; q < positions.size(); q++) {
                System.arraycopy(positions.get(q), 0, p, q * 12, 12);
                System.arraycopy(uvs.get(q), 0, t, q * 8, 8);
                System.arraycopy(normals.get(q), 0, n, q * 12, 12);
                System.arraycopy(colors.get(q), 0, c, q * 4, 4);
                System.arraycopy(lights.get(q), 0, l, q * 4, 4);
            }
            java.util.Arrays.fill(o, OverlayTexture.NO_OVERLAY);
            return new BakedMesh(p, t, n, c, o, l, stack.getItem(), null); // backport: no item_model component in 1.21.1
        }
    }

    public enum ItemMeshKey {
        CUTOUT_ITEMS_ATLAS(Layer.CUTOUT, false),
        CUTOUT_BLOCKS_ATLAS(Layer.CUTOUT, true),
        TRANSLUCENT_ITEMS_ATLAS(Layer.TRANSLUCENT, false),
        TRANSLUCENT_BLOCKS_ATLAS(Layer.TRANSLUCENT, true);

        private final Layer layer;
        private final boolean blocksAtlas;

        ItemMeshKey(Layer layer, boolean blocksAtlas) {
            this.layer = layer;
            this.blocksAtlas = blocksAtlas;
        }

        public static ItemMeshKey of(boolean translucent, boolean blocksAtlas) {
            if (translucent) {
                return blocksAtlas ? TRANSLUCENT_BLOCKS_ATLAS : TRANSLUCENT_ITEMS_ATLAS;
            }
            return blocksAtlas ? CUTOUT_BLOCKS_ATLAS : CUTOUT_ITEMS_ATLAS;
        }

        public Layer layer() {
            return layer;
        }

        public boolean blocksAtlas() {
            return blocksAtlas;
        }
    }

    /**
     * Result of {@link #bufferItem}: per-bucket meshes, glint flag, and the resolved model identity (the correct cache
     * key). {@code ownerDependent}: path reads holder state (use, pull, cooldown, main arm) => moves without a stack
     * change.
     */
    public record ItemMeshes(Map<ItemMeshKey, BakedMesh> meshes, boolean foil, float modelMinY, float modelZSize,
                             boolean stackDetermined, boolean ownerDependent, Object identity) {
    }
}
