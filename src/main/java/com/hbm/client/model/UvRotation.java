// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.backport.client.blockmodel.Quadrant;
import com.hbm.backport.client.blockmodel.CuboidFace;

public record UvRotation(CuboidFace.UVs uvs, Quadrant quadrant) {}
