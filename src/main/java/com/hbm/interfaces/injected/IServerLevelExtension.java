// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.capability.ResolvedCapCache;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.world.NtmWorldgenFields;
import java.util.List;

// backport: injected interface methods are default (throwing): javac only resolves
// an interface method on a concrete class read from bytecode if the method is not
// abstract, which is the same rule Loom and ModDevGradle interface injection state.
public interface IServerLevelExtension {

    default EndpointRegistry hbm$endpoints() {

        throw new AssertionError("backport: implemented by mixin");

    }
    default List<LevelNodeGraph<?>> hbm$graphs() {

        throw new AssertionError("backport: implemented by mixin");

    }
    default ResolvedCapCache hbm$resolvedCaps() {

        throw new AssertionError("backport: implemented by mixin");

    }
    default NtmWorldgenFields hbm$worldgenFields() {

        throw new AssertionError("backport: implemented by mixin");

    }
}
