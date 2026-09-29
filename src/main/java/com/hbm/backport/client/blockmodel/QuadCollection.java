// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * 26.x net.minecraft.client.resources.model.geometry.QuadCollection: baked quads split into
 * unculled ones and those culled by a face direction.
 */
public final class QuadCollection {

    public static final QuadCollection EMPTY = new Builder().build();
    private static final int SIDES = Direction.values().length;

    private final List<BakedQuad> all;
    private final List<BakedQuad> unculled;
    private final List<BakedQuad>[] culled;
    private final int materialFlags;

    private QuadCollection(List<BakedQuad> all, List<BakedQuad> unculled, List<BakedQuad>[] culled) {
        this.all = all;
        this.unculled = unculled;
        this.culled = culled;
        int flags = 0;
        for (BakedQuad quad : all) flags |= quad.materialInfo().flags();
        this.materialFlags = flags;
    }

    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        return direction == null ? unculled : culled[direction.ordinal()];
    }

    public List<BakedQuad> getAll() {
        return all;
    }

    public int materialFlags() {
        return materialFlags;
    }

    public boolean hasMaterialFlag(int flag) {
        return (materialFlags & flag) != 0;
    }

    public boolean isEmpty() {
        return all.isEmpty();
    }

    public static final class Builder {
        private final ImmutableList.Builder<BakedQuad> unculled = ImmutableList.builder();
        @SuppressWarnings("unchecked")
        private final ImmutableList.Builder<BakedQuad>[] culled = new ImmutableList.Builder[SIDES];

        public Builder() {
            for (int i = 0; i < SIDES; i++) culled[i] = ImmutableList.builder();
        }

        public Builder addCulledFace(Direction direction, BakedQuad quad) {
            culled[direction.ordinal()].add(quad);
            return this;
        }

        public Builder addUnculledFace(BakedQuad quad) {
            unculled.add(quad);
            return this;
        }

        public Builder add(@Nullable Direction direction, BakedQuad quad) {
            return direction == null ? addUnculledFace(quad) : addCulledFace(direction, quad);
        }

        public Builder addAll(QuadCollection other) {
            unculled.addAll(other.unculled);
            for (int i = 0; i < SIDES; i++) culled[i].addAll(other.culled[i]);
            return this;
        }

        @SuppressWarnings("unchecked")
        public QuadCollection build() {
            List<BakedQuad> u = unculled.build();
            List<BakedQuad>[] c = new List[SIDES];
            List<BakedQuad> all = new ArrayList<>(u);
            for (int i = 0; i < SIDES; i++) {
                c[i] = culled[i].build();
                all.addAll(c[i]);
            }
            return new QuadCollection(List.copyOf(all), u, c);
        }
    }
}
