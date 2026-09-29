// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.model.part.ModelTree;
import org.joml.Vector4fc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The vanilla identities a model offers a shaderpack guest, best match first. Without a pack, or with one that maps
 * none of them, the draw is unaffected. Equal delegate and list share an instancer.
 *
 * @see PackIdentity.Effect for the common lists
 * <p>backport: Flywheel 1.0 has no shaderpack-guest hook; the identities are carried but unused, the delegate draws as is.
 */
public record PackTaggedModel(Model delegate, List<PackIdentity> identities) implements Model {
    /**
     * {@code tree} with every node's model tagged.
     */
    public static ModelTree tag(ModelTree tree, List<PackIdentity> identities) {
        Map<String, ModelTree> children = new HashMap<>();
        for (int i = 0; i < tree.childCount(); i++) {
            children.put(tree.childName(i), tag(tree.child(i), identities));
        }
        Model model = tree.model();
        return new ModelTree(model == null ? null : new PackTaggedModel(model, identities), tree.initialPose(),
                children);
    }

    @Override
    public List<ConfiguredMesh> meshes() {
        return delegate.meshes();
    }

    @Override
    public Vector4fc boundingSphere() {
        return delegate.boundingSphere();
    }
}
