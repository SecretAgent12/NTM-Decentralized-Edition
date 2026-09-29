// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

import com.hbm.backport.client.flywheel.ExtraMemoryOps;
import com.hbm.backport.client.flywheel.SimpleInstanceType;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

/**
 * The instance types CrankShaft adds to Flywheel's InstanceTypes (UV_TRANSFORMED, CLIP_TRANSFORMED, BILLBOARD) and
 * AffineUvTransformedInstance.TYPE. backport: GLSL under assets/hbm/flywheel/instance/crankshaft/ (CrankShaft ships
 * them in its own flywheel namespace); writers copy the slab, then write the Flywheel 1.0 base fields over it.
 */
public final class CrankShaftInstanceTypes {
    private static final Matrix4f IDENTITY_M4 = new Matrix4f();

    public static final InstanceType<UvTransformedInstance> UV_TRANSFORMED =
            SimpleInstanceType.<UvTransformedInstance>builder(UvTransformedInstance::new)
                    .layout(LayoutBuilder.create()
                            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                            .vector("overlay", IntegerRepr.SHORT, 2)
                            .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
                            .matrix("pose", FloatRepr.FLOAT, 4)
                            .vector("uvRegion", FloatRepr.FLOAT, 4)
                            .build())
                    .seed(ptr -> {
                        seedTransformed(ptr);
                        // uvRegion default MUST be the identity remap (0,0,1,1); a zero-fill collapses every UV to a point.
                        ExtraMemoryOps.putVector4f(ptr + UvTransformedInstance.OFF_UV_REGION, 0.0f, 0.0f, 1.0f, 1.0f);
                    })
                    .writer((ptr, instance) -> {
                        instance.slab().copyTo(ptr);
                        writeTransformed(ptr, instance);
                    })
                    .vertexShader(rl("instance/crankshaft/transformed_uv.vert"))
                    .cullShader(flw("instance/cull/transformed.glsl"))
                    .build();

    public static final InstanceType<AffineUvTransformedInstance> AFFINE_UV_TRANSFORMED =
            SimpleInstanceType.<AffineUvTransformedInstance>builder(AffineUvTransformedInstance::new)
                    .layout(LayoutBuilder.create()
                            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                            .vector("overlay", IntegerRepr.SHORT, 2)
                            .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
                            .matrix("pose", FloatRepr.FLOAT, 4)
                            .vector("uvRegion", FloatRepr.FLOAT, 4)
                            .vector("uvShear", FloatRepr.FLOAT, 2)
                            .build())
                    .seed(ptr -> {
                        seedTransformed(ptr);
                        ExtraMemoryOps.putVector4f(ptr + UvTransformedInstance.OFF_UV_REGION, 0, 0, 1, 1);
                        MemoryUtil.memPutFloat(ptr + AffineUvTransformedInstance.OFF_UV_SHEAR, 0);
                        MemoryUtil.memPutFloat(ptr + AffineUvTransformedInstance.OFF_UV_SHEAR + 4, 0);
                    })
                    .writer((ptr, instance) -> {
                        instance.slab().copyTo(ptr);
                        writeTransformed(ptr, instance);
                    })
                    .vertexShader(rl("instance/crankshaft/transformed_affine_uv.vert"))
                    .cullShader(flw("instance/cull/transformed.glsl"))
                    .build();

    public static final InstanceType<ClipTransformedInstance> CLIP_TRANSFORMED =
            SimpleInstanceType.<ClipTransformedInstance>builder(ClipTransformedInstance::new)
                    .layout(LayoutBuilder.create()
                            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                            .vector("overlay", IntegerRepr.SHORT, 2)
                            .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
                            .matrix("pose", FloatRepr.FLOAT, 4)
                            .vector("slide", FloatRepr.FLOAT, 3)
                            .vector("plane", FloatRepr.FLOAT, 4)
                            .build())
                    .seed(CrankShaftInstanceTypes::seedTransformed)
                    .writer((ptr, instance) -> {
                        instance.slab().copyTo(ptr);
                        writeTransformed(ptr, instance);
                    })
                    .vertexShader(rl("instance/crankshaft/clip_transformed.vert"))
                    .cullShader(rl("instance/crankshaft/cull/clip_transformed.glsl"))
                    .build();

    public static final InstanceType<BillboardInstance> BILLBOARD =
            SimpleInstanceType.<BillboardInstance>builder(BillboardInstance::new)
                    .layout(LayoutBuilder.create()
                            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                            .vector("overlay", IntegerRepr.SHORT, 2)
                            .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
                            .vector("position", FloatRepr.FLOAT, 3)
                            .scalar("size", FloatRepr.FLOAT)
                            .vector("uvRegion", FloatRepr.FLOAT, 4)
                            .build())
                    .seed(ptr -> {
                        seedColoredLitOverlay(ptr);
                        ExtraMemoryOps.putVector4f(ptr + BillboardInstance.OFF_UV_REGION, 0.0f, 0.0f, 1.0f, 1.0f);
                    })
                    .writer((ptr, instance) -> {
                        instance.slab().copyTo(ptr);
                        writeColoredLitOverlay(ptr, instance);
                    })
                    .vertexShader(rl("instance/crankshaft/billboard.vert"))
                    .cullShader(rl("instance/crankshaft/cull/billboard.glsl"))
                    .build();

    private CrankShaftInstanceTypes() {}

    static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("hbm", path);
    }

    private static ResourceLocation flw(String path) {
        return ResourceLocation.fromNamespaceAndPath("flywheel", path);
    }

    static void seedColoredLitOverlay(long ptr) {
        MemoryUtil.memPutInt(ptr, 0xFFFFFFFF);
        ExtraMemoryOps.put2x16(ptr + 4, OverlayTexture.NO_OVERLAY);
    }

    static void seedTransformed(long ptr) {
        seedColoredLitOverlay(ptr);
        ExtraMemoryOps.putMatrix4f(ptr + 12, IDENTITY_M4);
    }

    /** Flywheel 1.0's ColoredLitOverlayInstance fields at CrankShaft's offsets (color 0, overlay 4, light 8). */
    public static void writeColoredLitOverlay(long ptr, ColoredLitOverlayInstance instance) {
        MemoryUtil.memPutByte(ptr, instance.red);
        MemoryUtil.memPutByte(ptr + 1, instance.green);
        MemoryUtil.memPutByte(ptr + 2, instance.blue);
        MemoryUtil.memPutByte(ptr + 3, instance.alpha);
        ExtraMemoryOps.put2x16(ptr + 4, instance.overlay);
        ExtraMemoryOps.put2x16(ptr + 8, instance.light);
    }

    /** As {@link #writeColoredLitOverlay}, plus the pose at 12. */
    public static void writeTransformed(long ptr, TransformedInstance instance) {
        writeColoredLitOverlay(ptr, instance);
        ExtraMemoryOps.putMatrix4f(ptr + 12, instance.pose);
    }
}
