// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.core.HumanoidRenderState;

// backport: 26.x extends ZombieRenderState (HumanoidRenderState + aggression); 1.21.1 reads the
// aggression from the entity in RenderUndeadSoldier's model
public final class UndeadSoldierRenderState extends HumanoidRenderState {

    public byte soldierType;
}
