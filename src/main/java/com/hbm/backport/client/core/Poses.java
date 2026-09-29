// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 26.x PoseStack.Pose helpers missing in 1.21.1. backport: kept for tree files rewritten by an earlier
 * rule; new rewrites go to the item model topic's PoseOps.
 */
public final class Poses {
    private Poses() {}

    /** 26.x {@code Pose.set(Pose)}: copies matrix and normal matrix; returns {@code target}. */
    public static PoseStack.Pose set(PoseStack.Pose target, PoseStack.Pose source) {
        target.pose().set(source.pose());
        target.normal().set(source.normal());
        return target;
    }
}
