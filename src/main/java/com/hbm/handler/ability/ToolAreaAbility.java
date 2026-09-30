// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.ability;

import com.hbm.data.ItemData;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.platform.Services;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public enum ToolAreaAbility implements BaseAbility, StringRepresentable {
    NONE("", 0) {
        @Override
        public boolean onDig(ToolDig dig, int abilityLevel) {
            return false;
        }
    },
    RECURSION("tool.ability.recursion", 1) {
        private static final int[] RADIUS = {3, 4, 5, 6, 7, 9, 10};

        @Override
        public int levels() {
            return RADIUS.length;
        }

        @Override
        public String extension(int level) {
            return " (" + RADIUS[level] + ")";
        }

        @Override
        public boolean allowed() {
            return ItemData.TOOL_ABILITY_VEIN.get();
        }

        @Override
        public boolean onDig(ToolDig dig, int abilityLevel) {
            ServerLevel level = dig.level();
            BlockState reference = level.getBlockState(dig.reference());

            if (reference.is(BlockTags.BASE_STONE_OVERWORLD)
                    && !ItemData.TOOL_RECURSION_STONE.get()) {
                return false;
            }
            if (reference.is(BlockTags.BASE_STONE_NETHER)
                    && !ItemData.TOOL_RECURSION_NETHERRACK.get()) {
                return false;
            }

            // backport-fix: BF-014 stone and deepslate variants of one ore (sharing a c:ores/<material>
            // item tag) are one vein; the original only matched the exact block (1.7.10 had no deepslate)
            List<TagKey<Item>> veinTags = oreTags(reference);
            int radius = RADIUS[abilityLevel];
            int maxDepth = Services.CONFIG.runtime().toolRecursionDepth();
            Set<BlockPos> visited = new HashSet<>();
            visited.add(dig.reference());

            Deque<Step> pending = new ArrayDeque<>();
            push(pending, dig.reference(), 1);

            while (!pending.isEmpty()) {
                Step step = pending.pop();
                if (step.depth() > maxDepth) continue;
                if (!visited.add(step.pos())) continue;
                if (step.pos().distSqr(dig.reference()) > (long) radius * radius) continue;
                BlockState candidate = level.getBlockState(step.pos());
                if (!candidate.is(reference.getBlock()) && !sharesOreTag(candidate, veinTags)) continue;
                if (dig.player().getMainHandItem().isEmpty()) return false;

                ItemToolAbility.breakExtraBlock(dig, step.pos());
                push(pending, step.pos(), step.depth() + 1);
            }

            return false;
        }

        private void push(Deque<Step> pending, BlockPos origin, int depth) {
            List<BlockPos> shuffled = new ArrayList<>(OFFSETS.size());
            for (BlockPos offset : OFFSETS) shuffled.add(origin.offset(offset));
            Collections.shuffle(shuffled);

            for (BlockPos next : shuffled) pending.push(new Step(next, depth));
        }

        record Step(BlockPos pos, int depth) {}
    },
    HAMMER("tool.ability.hammer", 2) {
        @Override
        public int levels() {
            return RANGE.length;
        }

        @Override
        public String extension(int level) {
            return " (" + RANGE[level] + ")";
        }

        @Override
        public boolean allowed() {
            return ItemData.TOOL_ABILITY_HAMMER.get();
        }

        @Override
        public boolean onDig(ToolDig dig, int abilityLevel) {
            int range = RANGE[abilityLevel];
            breakBox(dig, range, range, range);
            return false;
        }
    },
    HAMMER_FLAT("tool.ability.hammer_flat", 3) {
        @Override
        public int levels() {
            return RANGE.length;
        }

        @Override
        public String extension(int level) {
            return " (" + RANGE[level] + ")";
        }

        @Override
        public boolean allowed() {
            return ItemData.TOOL_ABILITY_HAMMER.get();
        }

        @Override
        public boolean onDig(ToolDig dig, int abilityLevel) {
            int range = RANGE[abilityLevel];

            if (!(dig.player().pick(dig.player().blockInteractionRange(), 0.0F, false)
                            instanceof BlockHitResult hit)
                    || hit.getType() != HitResult.Type.BLOCK) {
                return true;
            }

            switch (hit.getDirection().getAxis()) {
                case Y -> breakBox(dig, range, 0, range);
                case Z -> breakBox(dig, range, range, 0);
                case X -> breakBox(dig, 0, range, range);
            }

            return false;
        }
    },
    EXPLOSION("tool.ability.explosion", 4) {
        private static final float[] STRENGTH = {2.5F, 5F, 10F, 15F};

        @Override
        public int levels() {
            return STRENGTH.length;
        }

        @Override
        public String extension(int level) {
            return " (" + STRENGTH[level] + ")";
        }

        @Override
        public boolean allowsHarvest(int level) {
            return false;
        }

        @Override
        public boolean allowed() {
            return ItemData.TOOL_ABILITY_EXPLOSION.get();
        }

        @Override
        public boolean onDig(ToolDig dig, int abilityLevel) {
            ServerLevel level = dig.level();
            double x = dig.reference().getX() + 0.5D;
            double y = dig.reference().getY() + 0.5D;
            double z = dig.reference().getZ() + 0.5D;

            ExplosionVNT explosion =
                    new ExplosionVNT(level, x, y, z, STRENGTH[abilityLevel], dig.player());
            explosion.setBlockAllocator(new BlockAllocatorStandard());
            explosion.setBlockProcessor(new BlockProcessorStandard().setAllDrop());
            explosion.explode();

            level.explode(dig.player(), x, y, z, 0.1F, Level.ExplosionInteraction.NONE);
            return true;
        }
    };

    public static final ToolAreaAbility[] VALUES = values();
    public static final Codec<ToolAreaAbility> CODEC =
            StringRepresentable.fromEnum(ToolAreaAbility::values);
    public static final StreamCodec<ByteBuf, ToolAreaAbility> STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> VALUES[id], Enum::ordinal);

    private static final int SORT_ORDER_BASE = 0;
    private static final int[] RANGE = {1, 2, 3, 4};
    private static final List<BlockPos> OFFSETS = neighbourhood();

    private final String translationKey;
    private final int sortOrder;

    ToolAreaAbility(String translationKey, int order) {
        this.translationKey = translationKey;
        this.sortOrder = SORT_ORDER_BASE + order;
    }

    /** backport-fix: BF-014 the c:ores/<material> item tags of a block (empty for non-ores). */
    private static List<TagKey<Item>> oreTags(BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) return List.of();
        return new ItemStack(item).getTags()
                .filter(t -> t.location().getNamespace().equals("c") && t.location().getPath().startsWith("ores/"))
                .toList();
    }

    private static boolean sharesOreTag(BlockState state, List<TagKey<Item>> tags) {
        if (tags.isEmpty()) return false;
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) return false;
        ItemStack stack = new ItemStack(item);
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) return true;
        }
        return false;
    }

    private static List<BlockPos> neighbourhood() {
        List<BlockPos> offsets = new ArrayList<>(3 * 3 * 3 - 1);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static void breakBox(ToolDig dig, int xRange, int yRange, int zRange) {
        BlockPos reference = dig.reference();
        for (BlockPos pos :
                BlockPos.betweenClosed(
                        reference.offset(-xRange, -yRange, -zRange),
                        reference.offset(xRange, yRange, zRange))) {
            if (pos.equals(reference)) continue;
            ItemToolAbility.breakExtraBlock(dig, pos.immutable());
        }
    }

    public boolean allowsHarvest(int level) {
        return true;
    }

    public abstract boolean onDig(ToolDig dig, int abilityLevel);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String translationKey() {
        return translationKey;
    }

    @Override
    public int sortOrder() {
        return sortOrder;
    }
}
