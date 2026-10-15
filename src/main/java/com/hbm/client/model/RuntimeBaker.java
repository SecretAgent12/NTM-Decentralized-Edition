// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.backport.client.blockmodel.ModelBaker;
import com.hbm.backport.client.blockmodel.ResolvedModel;

/**
 * backport: upstream's RuntimeBaker snapshots the models and materials a lazily baking model
 * needs, because the 26.x ModelBaker (a MaterialBaker with interners) is only valid during the
 * model bake. DE's ModelBaker ({@code com.hbm.backport.client.blockmodel.Baker}) belongs to one
 * reload and stays valid after it (resolved models are cached, sprites come from the reload's
 * block atlas), so the models keep it as is, as beta.10 did.
 */
final class RuntimeBaker {
    private RuntimeBaker() {}

    static ModelBaker capture(ModelBaker source, ResolvedModel... models) {
        return source;
    }
}
