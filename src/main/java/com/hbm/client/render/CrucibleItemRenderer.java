// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.CrucibleItemVisual;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.HbmAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.hbm.lib.crankshaft.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.itemmodel.ItemModel;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.backport.client.itemmodel.SpecialModelRenderer;
import com.hbm.backport.client.blockmodel.ResolvableModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.LightTexture;
import com.hbm.backport.client.itemmodel.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.client.itemmodel.PoseOps;

public final class CrucibleItemRenderer
        implements SpecialModelRenderer<CrucibleItemRenderer.Argument>,
                ItemVisuals.Factory<CrucibleItemRenderer.Argument> {

    public static final int HILT = ResourceManager.crucible_sword.partId("Hilt");
    public static final int LEFT = ResourceManager.crucible_sword.partId("GuardLeft");
    public static final int RIGHT = ResourceManager.crucible_sword.partId("GuardRight");
    public static final int BLADE = ResourceManager.crucible_sword.partId("Blade");

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        float[] body = ResourceManager.crucible_sword.boundsOfParts(HILT, BLADE);
        Vector3f min = new Vector3f(body[0], body[1], body[2]);
        Vector3f max = new Vector3f(body[3] + 0.005F, body[4], body[5]);
        for (int part : new int[] {LEFT, RIGHT}) {
            float zCenter = part == LEFT ? 0.5F : -0.5F;
            float[] guard = ResourceManager.crucible_sword.boundsOfParts(part);
            float radius = 0;
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++)
                    radius =
                            Math.max(
                                    radius,
                                    (float)
                                            Math.hypot(
                                                    guard[y * 3 + 1] - 3,
                                                    guard[z * 3 + 2] - zCenter));
            min.min(new Vector3f(guard[0], 3 - radius, zCenter - radius));
            max.max(new Vector3f(guard[3], 3 + radius, zCenter + radius));
        }
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++)
                    output.accept(
                            new Vector3f(
                                    x == 0 ? min.x : max.x,
                                    y == 0 ? min.y : max.y,
                                    z == 0 ? min.z : max.z));
    }

    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        return null;
    }

    private static void part(
            SubmitNodeCollector collector, PoseStack pose, RenderType type, int part, int light) {
        collector.submitCustomGeometry(
                pose,
                type,
                (p, buffer) ->
                        ResourceManager.crucible_sword.renderPart(p, buffer, light, -1, part));
    }

    @Override
    public void submit(
            @Nullable Argument argument,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {
        if (argument == null) return;
        pose.pushPose();
        Plan plan = plan(argument.hand(), argument.charged(), argument.blocking(), pose.last());

        boolean gui = argument.context() == ItemDisplayContext.GUI;
        int bodyLight = gui ? LightTexture.FULL_BRIGHT : light;
        part(
                collector,
                pose,
                gui
                        ? FlatCutout.of(ResourceManager.crucible_hilt)
                        : RenderTypes.entityCutout(ResourceManager.crucible_hilt),
                HILT,
                bodyLight);
        RenderType guardType =
                gui
                        ? FlatCutout.of(ResourceManager.crucible_guard)
                        : RenderTypes.entityCutout(ResourceManager.crucible_guard);
        pose.pushPose();
        guardPose(pose.last(), true, plan.guardAngle());
        part(collector, pose, guardType, LEFT, bodyLight);
        pose.popPose();
        pose.pushPose();
        guardPose(pose.last(), false, plan.guardAngle());
        part(collector, pose, guardType, RIGHT, bodyLight);
        pose.popPose();
        if (plan.showBlade()) {
            bladePose(pose.last());
            part(
                    collector,
                    pose,
                    FlatCutout.of(ResourceManager.crucible_blade),
                    BLADE,
                    LightTexture.FULL_BRIGHT);
        }
        pose.popPose();
    }

    public static Plan plan(boolean hand, boolean charged, boolean blocking, PoseStack.Pose pose) {
        boolean swinging = false;
        double guardAngle = charged ? 0 : 90;
        boolean showBlade = charged;
        if (hand) {
            if (blocking) {

                PoseOps.translate(pose, 0F,
                        (float) (-0.125D / (0.3D * Math.sqrt(2))),
                        (float) (-0.375D / (0.3D * Math.sqrt(2))));
            } else {
                double[] rotation = HbmAnimations.getRelevantTransformation("SWING_ROT");
                double[] translation = HbmAnimations.getRelevantTransformation("SWING_TRANS");
                PoseOps.translate(pose, (float) translation[0], (float) translation[1], (float) translation[2]);
                PoseOps.rotate(pose, Axis.XP.rotationDegrees((float) rotation[0]));
                PoseOps.rotate(pose, Axis.ZP.rotationDegrees((float) rotation[2]));
                PoseOps.rotate(pose, Axis.YP.rotationDegrees((float) rotation[1]));
                swinging = rotation[0] != 0;
            }
            double[] guard = HbmAnimations.getRelevantTransformation("GUARD_ROT");
            guardAngle = !swinging && !charged ? 90 : guard[0];
            var inHand = Minecraft.getInstance().gameRenderer.itemInHandRenderer;
            showBlade =
                    inHand.mainHandHeight == 1
                            && inHand.oMainHandHeight == 1
                            && guard[2] == 0
                            && (swinging || charged);
        }
        return new Plan(guardAngle, showBlade);
    }

    public static void guardPose(PoseStack.Pose pose, boolean left, double angle) {
        float side = left ? 0.5F : -0.5F;
        PoseOps.translate(pose, 0F, 3F, side);
        PoseOps.rotate(pose, (left ? Axis.XN : Axis.XP).rotationDegrees((float) angle));
        PoseOps.translate(pose, 0F, -3F, -side);
    }

    public static void bladePose(PoseStack.Pose pose) {
        PoseOps.translate(pose, 0.005F, 0F, 0F);
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable Argument argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new CrucibleItemVisual(ctx, context, argument != null && argument.charged(), owner);
    }

    public record Plan(double guardAngle, boolean showBlade) {}

    public record Argument(
            ItemDisplayContext context, boolean hand, boolean charged, boolean blocking) {}

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
                    new CrucibleItemRenderer(),
                    (stack, display, owner) ->
                            new Argument(
                                    display,
                                    HandPass.local(display, owner),
                                    ((ItemCrucible) stack.getItem()).canOperate(stack),
                                    owner instanceof LivingEntity living && living.isBlocking()),
                    true,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
