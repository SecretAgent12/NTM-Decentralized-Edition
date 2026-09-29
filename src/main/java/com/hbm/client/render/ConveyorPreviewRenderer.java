// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.network.ConveyorBendableBlock;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ConveyorPlacer;
import com.hbm.items.tool.ConveyorRoute;
import com.hbm.items.tool.ConveyorRunData;
import com.hbm.items.tool.ItemConveyorWand;
import com.hbm.backport.client.rendertype.PrimitiveTopology;
import com.hbm.backport.client.rendertype.DepthStencilState;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.hbm.backport.client.blockmodel.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import com.hbm.backport.client.rendertype.BindGroupLayouts;
import com.hbm.backport.client.core.SubmitNodeCollector;
import net.minecraft.world.level.BlockAndTintGetter;
import com.hbm.backport.client.blockmodel.BlockStateModelSet;
import com.hbm.backport.client.blockmodel.BlockStateModel;
import com.hbm.backport.client.blockmodel.BlockStateModelPart;
import com.hbm.backport.client.rendertype.RenderSetup;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.core.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.hbm.backport.client.blockmodel.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.client.blockmodel.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.client.itemmodel.PoseOps;

public final class ConveyorPreviewRenderer {

    private static final int LAID = 0xFF00FFFF;
    private static final int REFUSED = 0xFFFF0000;

    private static final float YAW_STEP = 15F;

    public static final RenderPipeline PREVIEW_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(ParticleRenderTypes.MATRICES_NO_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_conveyor_preview")
                            .withVertexShader(ParticleRenderTypes.ENTITY_NO_FOG)
                            .withFragmentShader(ParticleRenderTypes.ENTITY_NO_FOG)
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT));
    private static final RenderType PREVIEW =
            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                    "ntm_conveyor_preview",
                    RenderSetup.builder(PREVIEW_PIPELINE)
                            .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                            .useLightmap()
                            .useOverlay()
                            .createRenderSetup());

    private static List<Cell> cells = List.of();

    private static int color = LAID;
    private static @Nullable ConveyorRunData lastRun;
    private static @Nullable BlockPos lastHit;
    private static @Nullable Direction lastSide;
    private static float lastYaw;

    private ConveyorPreviewRenderer() {}

    public static void submit(
            PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState levelState) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            clear();
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemConveyorWand)) {
            clear();
            return;
        }

        ConveyorRunData run = held.get(ModDataComponents.CONVEYOR_RUN.get());
        if (run == null
                || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            clear();
            return;
        }

        route(mc.level, player, held, run, hit);
        draw(poseStack, collector, levelState.cameraRenderState.pos);
    }

    private static void clear() {
        cells = List.of();
        lastRun = null;
        lastHit = null;
        lastSide = null;
    }

    private static void route(
            ClientLevel level,
            LocalPlayer player,
            ItemStack held,
            ConveyorRunData run,
            BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        Direction side = hit.getDirection();

        if (level.getBlockState(pos).getBlock() instanceof ConveyorBendableBlock bendable) {
            Direction moveDir = bendable.getInputDirection(level, pos);
            if (level.getBlockState(pos.relative(moveDir)).canBeReplaced()) side = moveDir;
        }

        if (run.equals(lastRun)
                && pos.equals(lastHit)
                && side == lastSide
                && Math.abs(lastYaw - player.getYRot()) < YAW_STEP) {
            return;
        }
        lastRun = run;
        lastHit = pos;
        lastSide = side;
        lastYaw = player.getYRot();

        Volume volume = new Volume();
        int laid =
                ConveyorRoute.construct(
                        level,
                        volume,
                        ItemConveyorWand.getType(held),
                        player,
                        run.anchor(),
                        run.side(),
                        pos,
                        side,
                        run.budget());

        color = laid > 0 ? LAID : REFUSED;
        bake(volume);
    }

    private static void bake(Volume volume) {
        BlockStateModelSet models =
                com.hbm.backport.client.blockmodel.BlockStateModelSet.current();
        RandomSource random = RandomSource.create();
        List<Cell> baked = new ArrayList<>(volume.cells.size());

        for (Long2ObjectMap.Entry<BlockState> entry : volume.cells.long2ObjectEntrySet()) {
            BlockPos pos = BlockPos.of(entry.getLongKey());
            BlockState state = entry.getValue();
            random.setSeed(state.getSeed(pos));
            baked.add(new Cell(pos, geometry(models.get(state), volume, pos, state, random)));
        }
        cells = List.copyOf(baked);
    }

    private static Geometry geometry(
            BlockStateModel model,
            Volume volume,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        int paint = color;

        List<BlockStateModelPart> parts = new ArrayList<>(1);
        model.collectParts(volume, pos, state, random, parts);

        QuadInstance instance = new QuadInstance();
        instance.setColor(paint);
        instance.setLightCoords(LightTexture.FULL_BRIGHT);
        instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

        return (pose, buf) -> {
            for (BlockStateModelPart part : parts) {
                for (Direction dir : Direction.VALUES) {
                    for (BakedQuad quad : part.getQuads(dir))
                        QuadInstance.putBakedQuad(buf, pose, quad, instance);
                }
                for (BakedQuad quad : part.getQuads(null)) QuadInstance.putBakedQuad(buf, pose, quad, instance);
            }
        };
    }

    private static void draw(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
        List<Cell> run = cells;
        if (run.isEmpty()) return;

        collector.submitCustomGeometry(
                poseStack,
                PREVIEW,
                (pose, buf) -> {
                    for (Cell cell : run) {
                        PoseStack.Pose at = pose.copy();
                        PoseOps.translate(at, (float) (cell.pos().getX() - camera.x),
                                (float) (cell.pos().getY() - camera.y),
                                (float) (cell.pos().getZ() - camera.z));
                        cell.geometry().emit(at, buf);
                    }
                });
    }

    @FunctionalInterface
    private interface Geometry {

        void emit(PoseStack.Pose pose, VertexConsumer buf);
    }

    private record Cell(BlockPos pos, Geometry geometry) {}

    private static final class Volume implements ConveyorPlacer, BlockAndTintGetter {

        private final Long2ObjectLinkedOpenHashMap<BlockState> cells =
                new Long2ObjectLinkedOpenHashMap<>();

        @Override
        public void place(BlockPos pos, BlockState state) {
            cells.put(pos.asLong(), state);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            BlockState state = cells.get(pos.asLong());
            return state == null ? Blocks.AIR.defaultBlockState() : state;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        // backport: 1.21.1 has no LevelLightEngine.EMPTY; an engine without sky or block light
        // reads 0 everywhere, as the 26.x empty engine does
        private static final LevelLightEngine NO_LIGHT =
                new LevelLightEngine(
                        new net.minecraft.world.level.chunk.LightChunkGetter() {
                            @Override
                            public net.minecraft.world.level.chunk.@Nullable LightChunk getChunkForLighting(
                                    int x, int z) {
                                return null;
                            }

                            @Override
                            public net.minecraft.world.level.BlockGetter getLevel() {
                                return net.minecraft.world.level.EmptyBlockGetter.INSTANCE;
                            }
                        },
                        false,
                        false);

        @Override
        public LevelLightEngine getLightEngine() {
            return NO_LIGHT;
        }

        // 26.x CardinalLighting.DEFAULT -> 1.21.1 directional shade (ClientLevel.getShade, normal sky)
        @Override
        public float getShade(net.minecraft.core.Direction direction, boolean shade) {
            if (!shade) return 1.0F;
            return switch (direction) {
                case DOWN -> 0.5F;
                case NORTH, SOUTH -> 0.8F;
                case WEST, EAST -> 0.6F;
                default -> 1.0F;
            };
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return -1;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public int getMinBuildHeight() {
            return 0;
        }
    }
}
