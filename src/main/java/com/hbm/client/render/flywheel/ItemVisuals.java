// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hbm.NuclearTech;
import com.hbm.registration.Reg;
import com.hbm.lib.crankshaft.ItemStackVisual;
import com.hbm.lib.crankshaft.ItemStackVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import com.hbm.backport.client.flywheel.VisualizerRegistry;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import com.hbm.backport.client.itemmodel.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import com.hbm.backport.client.itemmodel.ItemOwner;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ItemVisuals implements ItemStackVisualizer {
    private static final ItemVisuals INSTANCE = new ItemVisuals();
    private static final Set<String> TYPES =
            Set.of(
                    "bobble",
                    "boltgun",
                    "chainsaw",
                    "compressor",
                    "crucible",
                    "detonator_laser",
                    "fluid_tank",
                    "grenade",
                    "gun",
                    "gun_b92",
                    "lantern",
                    "mesh",
                    "missile_custom",
                    "plushie",
                    "radar_large",
                    "snowglobe");

    private ItemVisuals() {}

    public interface Factory<T> {
        ItemStackVisual createVisual(
                VisualizationContext ctx,
                @Nullable T argument,
                ItemStack stack,
                ItemDisplayContext context,
                @Nullable ItemOwner owner);
    }

    public static void register(ResourceManager resources) {
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(NuclearTech.MOD_ID)) continue;
            ResourceLocation model = Reg.itemModel(id);
            VisualizerRegistry.setVisualizer(
                    item, declares(resources, model == null ? id : model) ? INSTANCE : null);
        }
    }

    private static boolean declares(ResourceManager resources, ResourceLocation model) {
        Optional<Resource> resource =
                resources.getResource(model.withPath(path -> "items/" + path + ".json"));
        if (resource.isEmpty()) return false;
        try (Reader reader = resource.get().openAsReader()) {
            return declares(JsonParser.parseReader(reader));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean declares(JsonElement json) {
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) if (declares(element)) return true;
            return false;
        }
        if (!json.isJsonObject()) return false;
        for (var entry : json.getAsJsonObject().entrySet()) {
            JsonElement value = entry.getValue();
            if (entry.getKey().equals("type") && value.isJsonPrimitive()) {
                ResourceLocation type = ResourceLocation.tryParse(value.getAsString());
                if (type != null
                        && type.getNamespace().equals(NuclearTech.MOD_ID)
                        && TYPES.contains(type.getPath())) {
                    return true;
                }
            } else if (declares(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStackRenderState state = new ItemStackRenderState();
        com.hbm.backport.client.itemmodel.ItemModelResolver.get()
                .updateForTopItem(state, stack, context, minecraft.level, owner, 0);
        if (state.activeLayerCount == 0 || state.layers[0].specialRenderer == null) {
            return new BakedItemVisual(ctx, stack, context, owner);
        }
        if (state.activeLayerCount != 1
                || !(state.layers[0].specialRenderer instanceof Factory factory)) {
            throw new IllegalStateException(
                    stack + " resolves to " + state.layers[0].specialRenderer + " at " + context);
        }
        return factory.createVisual(
                ctx, state.layers[0].argumentForSpecialRendering, stack, context, owner);
    }
}
