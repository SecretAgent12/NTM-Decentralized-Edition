// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.client.render.flywheel.RadarLargeItemVisual;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.hbm.lib.crankshaft.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.itemmodel.ItemModel;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.backport.client.itemmodel.SpecialModelRenderer;
import com.hbm.backport.client.blockmodel.ResolvableModel;
import net.minecraft.resources.ResourceLocation;
import com.hbm.backport.client.itemmodel.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class RadarLargeItemRenderer
        implements SpecialModelRenderer<ItemDisplayContext>,
                ItemVisuals.Factory<ItemDisplayContext> {

    private static final String PART_RADAR = "Radar";
    private static final String PART_DISH = "Dish";
    public static final int RADAR = ResourceManager.radar_large.partId(PART_RADAR);
    public static final int DISH = ResourceManager.radar_large.partId(PART_DISH);

    private static final long SPIN_PERIOD = 3600L;
    private static final float SPIN_RATE = 0.1F;

    private final List<Vector3fc> extents = extentsOf();

    private static List<Vector3fc> extentsOf() {
        float[] bounds = ResourceManager.radar_large.boundsOfParts(PART_RADAR, PART_DISH);
        List<Vector3fc> corners = new ArrayList<>();
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) {
                    corners.add(new Vector3f(bounds[x * 3], bounds[y * 3 + 1], bounds[z * 3 + 2]));
                }
        return List.copyOf(corners);
    }

    public static float dishDegrees(long now) {
        return -(now % SPIN_PERIOD) * SPIN_RATE;
    }

    private static void part(
            SubmitNodeCollector collector, PoseStack pose, RenderType type, int name, int light) {
        collector.submitCustomGeometry(
                pose,
                type,
                (p, buffer) -> ResourceManager.radar_large.renderPart(p, buffer, light, -1, name));
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        extents.forEach(output);
    }

    @Override
    public @Nullable ItemDisplayContext extractArgument(ItemStack stack) {
        return ItemDisplayContext.NONE;
    }

    @Override
    public void submit(
            @Nullable ItemDisplayContext context,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {
        RenderType type = RenderTypes.entityCutoutCull(ResourceManager.radar_large_tex);

        part(collector, pose, type, RADAR, light);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(dishDegrees(GameTime.now())));
        part(collector, pose, type, DISH, light);
        pose.popPose();
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable ItemDisplayContext argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new RadarLargeItemVisual(ctx);
    }

    public record Unbaked(ResourceLocation base) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        instance ->
                                instance.group(
                                                ResourceLocation.CODEC
                                                        .fieldOf("base")
                                                        .forGetter(Unbaked::base))
                                        .apply(instance, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new DynamicSpecialWrapper<>(
                    new RadarLargeItemRenderer(),
                    (stack, display, owner) -> display,
                    true,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
