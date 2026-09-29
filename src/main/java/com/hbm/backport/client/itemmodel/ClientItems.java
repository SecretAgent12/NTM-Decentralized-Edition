// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * backport: loader for 26.x item definitions ({@code assets/hbm/items/<id>.json}), which 1.21.1
 * does not read.
 *
 * <ul>
 *   <li>{@link ModelEvent.RegisterAdditional}: parse every definition, register the models it depends
 *       on as standalone models so 1.21.1 bakes them.
 *   <li>{@link ModelEvent.ModifyBakingResult}: an item whose definition is a plain {@code
 *       minecraft:model} gets that baked model as its inventory model (native 1.21.1 rendering; tints
 *       via the ItemColor bridge). Every other item gets {@link ItemDefinitionRenderer.Model}, a
 *       custom-renderer model drawn by the {@link ItemDefinitionRenderer} BEWLR.
 *   <li>{@link ModelEvent.BakingCompleted} (render thread): bake the 26.x {@link ItemModel} trees used
 *       by {@link ItemModelResolver}.
 * </ul>
 */
public final class ClientItems {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> NAMESPACES = Set.of("hbm");
    private static final FileToIdConverter LISTER = FileToIdConverter.json("items");

    /** 26.x {@code ClientItem}: the top of an item definition file. */
    public record Definition(ItemModel.Unbaked model, boolean oversizedInGui, boolean handAnimationOnSwap) {
        public static final Codec<Definition> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                ItemModels.CODEC.fieldOf("model").forGetter(Definition::model),
                                                Codec.BOOL.optionalFieldOf("oversized_in_gui", false).forGetter(Definition::oversizedInGui),
                                                Codec.BOOL.optionalFieldOf("hand_animation_on_swap", true).forGetter(Definition::handAnimationOnSwap))
                                        .apply(i, Definition::new));
    }

    private static volatile Map<ResourceLocation, Definition> definitions = Map.of();
    private static volatile Map<Item, ItemModel> models = Map.of();
    private static volatile Map<ResourceLocation, ItemModel> modelsById = Map.of();
    private static volatile ItemModel missingItemModel = EmptyModel.INSTANCE;
    private static volatile Map<Item, Definition> definitionsByItem = Map.of();
    private static volatile Map<Item, ItemTintSource[]> nativeTints = Map.of();

    private ClientItems() {}

    public static @Nullable ItemModel model(Item item) {
        return models.get(item);
    }

    /** 26.x {@code ModelManager.getItemModel(id)}: the baked model of an item definition id. */
    public static ItemModel itemModel(ResourceLocation id) {
        ItemModel model = modelsById.get(id);
        return model != null ? model : missingItemModel;
    }

    public static boolean oversizedInGui(Item item) {
        Definition d = definitionsByItem.get(item);
        return d != null && d.oversizedInGui();
    }

    public static boolean hasDefinition(Item item) {
        return definitionsByItem.containsKey(item);
    }

    /** ItemColor bridge for items rendered natively from a plain minecraft:model with tints. */
    public static int nativeTint(ItemStack stack, int tintIndex) {
        ItemTintSource[] tints = nativeTints.get(stack.getItem());
        if (tints == null || tintIndex < 0 || tintIndex >= tints.length) return -1;
        // backport: 1.21.1 ItemColor has no holder; the tint source gets no owner
        return tints[tintIndex].calculate(stack, Minecraft.getInstance().level, null);
    }

    static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        Map<ResourceLocation, Definition> defs = new LinkedHashMap<>();
        Set<ResourceLocation> dependencies = new LinkedHashSet<>();
        var resources = Minecraft.getInstance().getResourceManager();
        for (Map.Entry<ResourceLocation, Resource> entry : LISTER.listMatchingResources(resources).entrySet()) {
            ResourceLocation id = LISTER.fileToId(entry.getKey());
            if (!NAMESPACES.contains(id.getNamespace())) continue;
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                Definition definition =
                        Definition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(IllegalStateException::new);
                defs.put(id, definition);
                definition.model().resolveDependencies(dependencies::add);
            } catch (Exception e) {
                LOGGER.error("Failed to load item definition {}", entry.getKey(), e);
            }
        }
        definitions = defs;
        for (ResourceLocation dependency : dependencies)
            event.register(ModelResourceLocation.standalone(dependency));
    }

    static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> baked = event.getModels();
        BakedModel missing = baked.get(ModelBakery.MISSING_MODEL_VARIANT);
        Map<Item, Definition> byItem = new IdentityHashMap<>();
        Map<Item, ItemTintSource[]> tints = new IdentityHashMap<>();
        for (Map.Entry<ResourceLocation, Definition> entry : definitions.entrySet()) {
            ResourceLocation id = entry.getKey();
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) continue;
            Definition definition = entry.getValue();
            byItem.put(item, definition);
            ModelResourceLocation inventory = ModelResourceLocation.inventory(id);
            if (definition.model() instanceof BlockModelWrapper.Unbaked plain) {
                BakedModel model = baked.get(ModelResourceLocation.standalone(plain.model()));
                if (model != null) baked.put(inventory, model);
                if (!plain.tints().isEmpty()) tints.put(item, plain.tints().toArray(new ItemTintSource[0]));
            } else {
                List<ResourceLocation> deps = new ArrayList<>();
                definition.model().resolveDependencies(deps::add);
                BakedModel base = null;
                for (ResourceLocation dep : deps) {
                    base = baked.get(ModelResourceLocation.standalone(dep));
                    if (base != null) break;
                }
                baked.put(inventory, new ItemDefinitionRenderer.Model(base != null ? base : missing));
            }
        }
        definitionsByItem = byItem;
        nativeTints = tints;
    }

    static void onBakingCompleted(ModelEvent.BakingCompleted event) {
        ItemQuads.clearCache();
        Map<ModelResourceLocation, BakedModel> baked = event.getModels();
        BakedModel missing = event.getModelManager().getMissingModel();
        ItemModel missingItem = new BlockModelWrapper(missing, List.of(), new Matrix4f());
        ItemModel.BakingContext context =
                new ItemModel.BakingContext(
                        id -> baked.get(ModelResourceLocation.standalone(id)), missing, missingItem);
        Map<Item, ItemModel> out = new IdentityHashMap<>();
        Map<ResourceLocation, ItemModel> outById = new HashMap<>();
        Map<ResourceLocation, Definition> defs = new HashMap<>(definitions);
        for (Map.Entry<ResourceLocation, Definition> entry : defs.entrySet()) {
            ItemModel model;
            try {
                model = entry.getValue().model().bake(context, new Matrix4f());
            } catch (Exception e) {
                LOGGER.error("Failed to bake item model {}", entry.getKey(), e);
                model = missingItem;
            }
            outById.put(entry.getKey(), model);
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            if (item != Items.AIR) out.put(item, model);
        }
        missingItemModel = missingItem;
        modelsById = outById;
        models = out;
    }
}
