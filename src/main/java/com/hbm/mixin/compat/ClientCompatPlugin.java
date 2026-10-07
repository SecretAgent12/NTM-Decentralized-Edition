// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class ClientCompatPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        // backport: the sodium mixins are gated like the Iris ones. They are @Pseudo and name
        // their targets by string (no Sodium on the backport's compile classpath), so this only
        // keeps them (and the duck interfaces they add) from being considered without Sodium.
        // backport: JEI fluid tooltips (BF-031), only with JEI installed
        if (mixin.contains(".jei."))
            return getClass()
                            .getClassLoader()
                            .getResource("mezz/jei/neoforge/platform/FluidHelper.class")
                    != null;
        // backport-fix: BF-069 Create's JEI Item Drain page, only with Create installed
        if (mixin.contains(".create."))
            return getClass()
                            .getClassLoader()
                            .getResource(
                                    "com/simibubi/create/compat/jei/category/ItemDrainCategory.class")
                    != null;
        if (mixin.contains(".sodium."))
            return getClass()
                            .getClassLoader()
                            .getResource("net/caffeinemc/mods/sodium/client/SodiumClientMod.class")
                    != null;
        return !mixin.contains(".iris.")
                || getClass().getClassLoader().getResource("net/irisshaders/iris/Iris.class")
                        != null;
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> mine, Set<String> other) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {}

    @Override
    public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
}
