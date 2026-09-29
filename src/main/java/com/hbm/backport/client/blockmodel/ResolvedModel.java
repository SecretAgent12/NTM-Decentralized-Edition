// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 26.x net.minecraft.client.resources.model.ResolvedModel over a 1.21.1 unbaked model whose
 * parent chain is resolved: "top" values are the ones the chain resolves to.
 *
 * <p>Geometry: HBM custom geometry ({@link GeometryAdapter}, e.g. hbm:obj) bakes through its
 * 26.x {@link UnbakedGeometry}; plain element models bake face by face through the 1.21.1
 * FaceBakery (via BlockModel.bakeFace, so NeoForge face data applies). A quad's chunk layer comes
 * from the model's NeoForge render_type hint when it has one, else from its sprite's
 * transparency (26.x per-quad layers).
 */
public final class ResolvedModel implements ModelDebugName {
    private static final Logger LOGGER = LoggerFactory.getLogger("hbm/blockmodel");
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private final ResourceLocation id;
    private final UnbakedModel wrapped;

    public ResolvedModel(ResourceLocation id, UnbakedModel wrapped) {
        this.id = id;
        this.wrapped = wrapped;
    }

    public ResourceLocation id() {
        return id;
    }

    public UnbakedModel wrapped() {
        return wrapped;
    }

    public @Nullable BlockModel blockModel() {
        return wrapped instanceof BlockModel model ? model : null;
    }

    @Override
    public String debugName() {
        return id.toString();
    }

    public TextureSlots getTopTextureSlots() {
        BlockModel model = blockModel();
        return model == null ? TextureSlots.EMPTY : TextureSlots.of(model);
    }

    public boolean getTopAmbientOcclusion() {
        BlockModel model = blockModel();
        return model == null || model.hasAmbientOcclusion();
    }

    public BlockModel.@Nullable GuiLight getTopGuiLight() {
        BlockModel model = blockModel();
        return model == null ? BlockModel.GuiLight.SIDE : model.getGuiLight();
    }

    public ItemTransforms getTopTransforms() {
        BlockModel model = blockModel();
        return model == null ? ItemTransforms.NO_TRANSFORMS : model.getTransforms();
    }

    public Material.Baked resolveParticleMaterial(TextureSlots slots, ModelBaker baker) {
        return baker.materials().resolveSlot(slots, "particle", this);
    }

    public UnbakedGeometry getTopGeometry() {
        return (slots, baker, state, name) -> bakeTopGeometry(slots, baker, state);
    }

    /** The NeoForge render_type hint of the model chain as a transparency, or null. */
    public @Nullable Transparency renderTypeHint() {
        BlockModel model = blockModel();
        if (model == null) return null;
        ResourceLocation hint = model.customData.getRenderTypeHint();
        if (hint == null) return null;
        return switch (hint.getPath()) {
            case "solid" -> Transparency.NONE;
            case "cutout", "cutout_mipped", "cutout_mipped_all", "tripwire" -> Transparency.TRANSPARENT;
            case "translucent" -> Transparency.TRANSLUCENT;
            default -> null;
        };
    }

    public QuadCollection bakeTopGeometry(TextureSlots slots, ModelBaker baker, ModelState state) {
        BlockModel model = blockModel();
        if (model == null) return QuadCollection.EMPTY;
        if (model.customData.hasCustomGeometry()) {
            if (model.customData.getCustomGeometry() instanceof GeometryAdapter adapter)
                return adapter.geometry().bake(slots, baker, state, this);
            if (WARNED.add(debugName()))
                LOGGER.warn("{} uses a custom loader HBM cannot bake as 26.x geometry; drawing nothing", debugName());
            return QuadCollection.EMPTY;
        }
        Transparency hint = renderTypeHint();
        net.minecraft.client.resources.model.ModelState vanillaState = state.vanilla();
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (BlockElement element : model.getElements()) {
            for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                Direction dir = entry.getKey();
                BlockElementFace face = entry.getValue();
                Material.Baked material = baker.materials().resolveSlot(slots, face.texture(), this);
                BakedQuad raw = BakedQuad.fromVanilla(
                        BlockModel.bakeFace(element, face, material.sprite(), dir, vanillaState));
                BakedQuad.MaterialInfo info = BakedQuad.MaterialInfo.of(
                        material,
                        hint != null ? hint : material.transparency(),
                        face.tintIndex(),
                        element.shade,
                        raw.materialInfo().lightEmission(),
                        raw.materialInfo().ambientOcclusion());
                BakedQuad quad = raw.withMaterialInfo(info);
                if (face.cullForDirection() == null) builder.addUnculledFace(quad);
                else builder.addCulledFace(
                        Direction.rotate(state.transformation().getMatrix(), face.cullForDirection()), quad);
            }
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "ResolvedModel[" + id + "]";
    }
}
