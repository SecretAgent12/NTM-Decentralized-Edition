// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.InstanceWriter;
import dev.engine_room.flywheel.api.layout.Layout;
import java.util.Objects;
import java.util.function.LongConsumer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * backport: CrankShaft's SimpleInstanceType (26.x) builds slot-writing types from a layout and a {@code seed}; Flywheel
 * 1.0's needs an {@link InstanceWriter}. This one accepts both: without a writer, instances must be
 * {@link Slab.Holder}s and their slab is copied into the slot.
 */
public final class SimpleInstanceType<I extends Instance> implements InstanceType<I> {
    private final Factory<I> factory;
    private final Layout layout;
    private final InstanceWriter<I> writer;
    private final @Nullable LongConsumer seed;
    private final ResourceLocation vertexShader;
    private final ResourceLocation cullShader;

    public SimpleInstanceType(Factory<I> factory, Layout layout, @Nullable InstanceWriter<I> writer,
                              @Nullable LongConsumer seed, ResourceLocation vertexShader, ResourceLocation cullShader) {
        this.factory = factory;
        this.layout = layout;
        this.writer = writer != null ? writer : SimpleInstanceType::writeSlab;
        this.seed = seed;
        this.vertexShader = vertexShader;
        this.cullShader = cullShader;
    }

    /**
     * Default writer: the slab, then the Flywheel 1.0 base fields a slab instance may also carry (CrankShaft keeps
     * colour/overlay/light/pose in the slot too, at the same offsets: 0 / 4 / 8 / 12).
     */
    private static void writeSlab(long ptr, Instance instance) {
        ((Slab.Holder) instance).slab().copyTo(ptr);
        if (instance instanceof dev.engine_room.flywheel.lib.instance.TransformedInstance transformed) {
            com.hbm.lib.crankshaft.CrankShaftInstanceTypes.writeTransformed(ptr, transformed);
        } else if (instance instanceof dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance lit) {
            com.hbm.lib.crankshaft.CrankShaftInstanceTypes.writeColoredLitOverlay(ptr, lit);
        }
    }

    public static <I extends Instance> Builder<I> builder(Factory<I> factory) {
        return new Builder<>(factory);
    }

    @Override
    public I create(InstanceHandle handle) {
        return factory.create(this, handle);
    }

    @Override
    public Layout layout() {
        return layout;
    }

    @Override
    public InstanceWriter<I> writer() {
        return writer;
    }

    public @Nullable LongConsumer seed() {
        return seed;
    }

    @Override
    public ResourceLocation vertexShader() {
        return vertexShader;
    }

    @Override
    public ResourceLocation cullShader() {
        return cullShader;
    }

    @FunctionalInterface
    public interface Factory<I extends Instance> {
        I create(InstanceType<I> type, InstanceHandle handle);
    }

    public static final class Builder<I extends Instance> {
        private final Factory<I> factory;
        private @Nullable Layout layout;
        private @Nullable InstanceWriter<I> writer;
        private @Nullable LongConsumer seed;
        private @Nullable ResourceLocation vertexShader;
        private @Nullable ResourceLocation cullShader;

        public Builder(Factory<I> factory) {
            this.factory = factory;
        }

        public Builder<I> layout(Layout layout) {
            this.layout = layout;
            return this;
        }

        public Builder<I> writer(InstanceWriter<I> writer) {
            this.writer = writer;
            return this;
        }

        public Builder<I> seed(LongConsumer seed) {
            this.seed = seed;
            return this;
        }

        public Builder<I> vertexShader(ResourceLocation vertexShader) {
            this.vertexShader = vertexShader;
            return this;
        }

        public Builder<I> cullShader(ResourceLocation cullShader) {
            this.cullShader = cullShader;
            return this;
        }

        public SimpleInstanceType<I> build() {
            Objects.requireNonNull(layout, "layout");
            Objects.requireNonNull(vertexShader, "vertexShader");
            Objects.requireNonNull(cullShader, "cullShader");
            return new SimpleInstanceType<>(factory, layout, writer, seed, vertexShader, cullShader);
        }
    }
}
