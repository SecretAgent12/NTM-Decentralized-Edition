// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

/**
 * 26.x {@code net.minecraft.client.renderer.SubmitNodeStorage}. backport: a discarding sink (see
 * {@link SubmitNodeCollection}); subclasses override the submissions they want to observe.
 */
public class SubmitNodeStorage extends SubmitNodeCollection implements SubmitNodeCollector {
    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return new SubmitNodeCollection();
    }
}
