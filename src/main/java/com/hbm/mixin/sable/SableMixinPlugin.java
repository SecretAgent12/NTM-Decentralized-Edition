// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.sable;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** backport: applies hbm-sable.mixins.json only when Sable is installed. */
public final class SableMixinPlugin implements IMixinConfigPlugin {
    private static final boolean SABLE = present();

    private static boolean present() {
        try {
            LoadingModList mods = LoadingModList.get();
            if (mods != null) return mods.getModFileById("sable") != null;
        } catch (Throwable ignored) {
            // fall back to the class lookup below
        }
        return SableMixinPlugin.class.getClassLoader()
                        .getResource("dev/ryanhcode/sable/api/block/BlockSubLevelAssemblyListener.class")
                != null;
    }

    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        return SABLE;
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
