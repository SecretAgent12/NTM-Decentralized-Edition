// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import com.hbm.backport.client.core.EntityModel;
import com.hbm.backport.client.core.LivingEntityRenderState;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;

/**
 * backport: 26.x vanilla mob models (CreeperModel, AdultChickenModel, SilverfishModel, BlazeModel) are posed from a
 * render state; 1.21.1's are entity-posed ({@code setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw,
 * headPitch)}). This adapter builds the 1.21.1 model on the same root and poses it from the state's fields. The four
 * vanilla models used this way never read the entity argument (null is passed). {@code ageInTicks} is per model:
 * the chicken's is ChickenRenderer#getBob.
 */
public final class VanillaEntityModel<S extends LivingEntityRenderState> extends EntityModel<S> {
    private final net.minecraft.client.model.EntityModel<?> vanilla;
    private final ToFloatFunction<S> ageInTicks;

    public VanillaEntityModel(ModelPart root, Function<ModelPart, ? extends net.minecraft.client.model.EntityModel<?>> vanilla,
                              ToFloatFunction<S> ageInTicks) {
        super(root);
        this.vanilla = vanilla.apply(root);
        this.ageInTicks = ageInTicks;
    }

    public static <S extends LivingEntityRenderState> Function<ModelPart, EntityModel<? super S>> of(
            Function<ModelPart, ? extends net.minecraft.client.model.EntityModel<?>> vanilla) {
        return root -> new VanillaEntityModel<S>(root, vanilla, state -> state.ageInTicks);
    }

    public static <S extends LivingEntityRenderState> Function<ModelPart, EntityModel<? super S>> of(
            Function<ModelPart, ? extends net.minecraft.client.model.EntityModel<?>> vanilla, ToFloatFunction<S> ageInTicks) {
        return root -> new VanillaEntityModel<S>(root, vanilla, ageInTicks);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setupAnim(S state) {
        super.setupAnim(state);
        vanilla.young = state.isBaby;
        ((net.minecraft.client.model.EntityModel) vanilla).setupAnim(null, state.walkAnimationPos,
                state.walkAnimationSpeed, ageInTicks.applyAsFloat(state), state.yRot, state.xRot);
    }
}
