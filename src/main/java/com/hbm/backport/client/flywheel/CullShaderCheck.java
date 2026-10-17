// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import com.hbm.client.particle.ParticleInstance;
import com.hbm.client.render.flywheel.BeamInstance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.Layout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * backport-fix: BF-086 — every instance layout needs a cull shader file of its own.
 *
 * <p>Flywheel 1.0 caches compiled shaders by name, and names an indirect cull shader after its file only
 * ({@code "culling/" + cullShader}), while the code it compiles embeds the instance layout (record size, field
 * offsets). Two types with different layouts and one cull file therefore share the cull shader of whichever was
 * compiled first; the other reads its instances with the wrong stride and gets culled almost at random (dashed
 * wires, foundry, conveyor belt, text, hazard diamonds, or the other way round most of the mod). CrankShaft (the
 * Flywheel fork ntm-next uses) culls all types in one uber shader, so ntm-next never had this. This check logs an
 * error at start-up if such a pair appears again among the types the mod uses.
 */
public final class CullShaderCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger("hbm-backport");

    private CullShaderCheck() {}

    public static void run() {
        List<InstanceType<?>> types =
                List.of(
                        InstanceTypes.TRANSFORMED,
                        InstanceTypes.POSED,
                        InstanceTypes.ORIENTED,
                        InstanceTypes.SHADOW,
                        InstanceTypes.UV_TRANSFORMED,
                        com.hbm.lib.crankshaft.CrankShaftInstanceTypes.AFFINE_UV_TRANSFORMED,
                        InstanceTypes.CLIP_TRANSFORMED,
                        InstanceTypes.BILLBOARD,
                        BeamInstance.SOLID,
                        BeamInstance.LINE,
                        ParticleInstance.TYPE,
                        ParticleInstance.GROW_IN,
                        ParticleInstance.HADRON,
                        ParticleInstance.UPRIGHT,
                        ParticleInstance.RBMK_JET,
                        ParticleInstance.CASING_SMOKE);
        Map<ResourceLocation, InstanceType<?>> byCull = new HashMap<>();
        for (InstanceType<?> type : types) {
            InstanceType<?> other = byCull.putIfAbsent(type.cullShader(), type);
            if (other != null && !sameLayout(other.layout(), type.layout())) {
                LOGGER.error(
                        "Flywheel instance types {} and {} share the cull shader {} but not the layout ({} vs {} bytes); "
                                + "with the indirect backend one of them would be culled with the other's layout",
                        other, type, type.cullShader(), other.layout().byteSize(), type.layout().byteSize());
            }
        }
    }

    private static boolean sameLayout(Layout a, Layout b) {
        return a.byteSize() == b.byteSize() && a.elements().equals(b.elements());
    }
}
