// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

/**
 * 26.x {@code com.mojang.blaze3d.platform.PolygonMode}. backport: 1.21.1 render types have no polygon
 * mode state; WIREFRAME pipelines draw filled.
 */
public enum PolygonMode {
    FILL,
    WIREFRAME
}
