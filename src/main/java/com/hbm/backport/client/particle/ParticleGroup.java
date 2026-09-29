// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.particle;

import java.util.AbstractQueue;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.particle.ParticleGroup}: owns the particles of one {@link
 * ParticleRenderType} key and extracts them for rendering.
 *
 * <p>backport: 1.21.1's ParticleEngine keeps one queue per render type and ticks/evicts/clears it
 * itself. {@link #particles} is a live read view of the engine's queue for this group's key (the
 * engine replaces its queues on level change), so group code iterates exactly what the engine holds.
 * Admission/tick hooks that 26.x runs in {@code ParticleGroup.add/tickParticle} are driven from
 * {@code ParticleEngineBridgeMixin} (see {@link ParticleBridge#onAdmitted}).
 */
public abstract class ParticleGroup<P extends Particle> {

    protected final ParticleEngine engine;
    protected final Queue<P> particles = new EngineQueueView();
    @Nullable ParticleRenderType key;

    protected ParticleGroup(ParticleEngine engine) {
        this.engine = engine;
    }

    public abstract ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks);

    public boolean isEmpty() {
        return particles.isEmpty();
    }

    public int size() {
        return particles.size();
    }

    @SuppressWarnings("unchecked")
    private Queue<P> backing() {
        Queue<Particle> q = key == null ? null : ParticleBridge.queue(engine, key);
        return q == null ? (Queue<P>) (Queue<?>) EMPTY : (Queue<P>) (Queue<?>) q;
    }

    private static final Queue<Particle> EMPTY = new java.util.ArrayDeque<>(0);

    private final class EngineQueueView extends AbstractQueue<P> {
        @Override
        public Iterator<P> iterator() {
            Queue<P> q = backing();
            return q.isEmpty() ? Collections.emptyIterator() : q.iterator();
        }

        @Override
        public int size() {
            return backing().size();
        }

        /** backport: particles enter through {@code ParticleEngine.add}; the view only reads. */
        @Override
        public boolean offer(P p) {
            engine.add(p);
            return true;
        }

        @Override
        public P poll() {
            return backing().poll();
        }

        @Override
        public P peek() {
            return backing().peek();
        }
    }
}
