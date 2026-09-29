// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

/**
 * Implemented by an EntityRenderer or BlockEntityRenderer whose render-state creation and extraction
 * may run on Flywheel worker threads, concurrently with other extractions, while the render thread waits.
 * <p>
 * Contract: reads only the extracted object, its level, other entities and state fixed at construction; writes only
 * the render state (idempotent lazy caches excepted); never touches GL, textures, fonts or other render-thread-only
 * objects. Subclasses inherit the claim.
 * <p>
 * backport: Flywheel 1.0 / Minecraft 1.21.1 never extract concurrently; the interface is kept as a marker and
 * {@code supports} is always {@code false} (every caller then falls back to the render thread).
 */
public interface ConcurrentRenderStateExtraction {
    static boolean supports(Object renderer) {
        return false;
    }
}
