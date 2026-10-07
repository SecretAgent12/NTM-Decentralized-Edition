// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.fluid;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * backport-fix: BF-070 — the fluid type of a classic fluid that behaves like water (sulfuric acid)
 * or lava (volcanic lava, radioactive lava).
 *
 * <p>These fluids used vanilla's own {@code minecraft:water} / {@code minecraft:lava} types. In 26.x
 * (where NTM: NEXT is made) a fluid's look comes from a model per fluid, so sharing the vanilla type
 * was harmless; in 1.21.1 the textures and tint belong to the fluid type, so placed sulfuric acid
 * looked like plain water and the lavas like plain lava. Each such fluid now gets its own type with
 * water's or lava's movement properties, so it can carry its own textures (registered in
 * NuclearTechNeoForgeClient). Burning, extinguishing and the fluid tags stay in {@link
 * ClassicFluid}, unchanged.
 */
public final class ClassicLiquidType extends FluidType {

    private final ClassicFluid.Spec spec;

    private ClassicLiquidType(ClassicFluid.Spec spec, Properties properties) {
        super(properties);
        this.spec = spec;
    }

    public static ClassicLiquidType waterLike(ClassicFluid.Spec spec) {
        return new ClassicLiquidType(
                spec,
                Properties.create()
                        .fallDistanceModifier(0F)
                        .canExtinguish(true)
                        .supportsBoating(true)
                        .canHydrate(true)
                        .density(spec.density())
                        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                        .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH));
    }

    public static ClassicLiquidType lavaLike(ClassicFluid.Spec spec) {
        return new ClassicLiquidType(
                spec,
                Properties.create()
                        .canSwim(false)
                        .canDrown(false)
                        .pathType(PathType.LAVA)
                        .adjacentPathType(null)
                        .lightLevel(15)
                        .density(3000)
                        .viscosity(6000)
                        .temperature(1300)
                        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA));
    }

    @Override
    public String getDescriptionId() {
        return spec.block().get().getDescriptionId();
    }
}
