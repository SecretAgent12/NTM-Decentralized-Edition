// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.ct;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.Logger;

/**
 * backport-fix: BF-105 built-in connected textures, as in 1.7.10 (render/block/ct, IBlockCT): no extra mod needed.
 *
 * <p>NTM: NEXT connects textures only through Continuity or Fusion (built-in resource packs that are added when one
 * of them is installed); without them every CT block shows its plain texture. Here the block-state models of the
 * blocks listed in {@code assets/<namespace>/connected_textures.json} are wrapped in {@link CtBakedModel}, which
 * splits every face that uses a listed texture into four quarters and picks each quarter from the plain texture or
 * from its {@code _ct} sheet, exactly like 1.7.10's CTContext/IconCT. The {@code _ct} sheets are the 1.7.10 ones.
 *
 * <p>Config format, one entry per block:
 * <pre>
 * "hbm:deco_steel": {
 *   "connect": ["hbm:deco_steel", "hbm:deco_rusty_steel"],     blocks this one joins (IBlockCT.canConnect)
 *   "textures": {"hbm:block/deco_steel": "hbm:block/ct/deco_steel_ct"}   plain texture -> its _ct sheet
 * }
 * </pre>
 * Files of all namespaces are read, so an addon can add its own blocks.
 */
public final class ConnectedTextures {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG = "connected_textures.json";

    private ConnectedTextures() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ConnectedTextures::wrapModels);
    }

    private record Entry(Set<Block> connect, Map<ResourceLocation, ResourceLocation> textures) {}

    private static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<Block, Entry> entries = load();
        Function<Material, TextureAtlasSprite> sprites = event.getTextureGetter();
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        int wrapped = 0;
        for (Map.Entry<Block, Entry> e : entries.entrySet()) {
            Map<ResourceLocation, TextureAtlasSprite> sheets = new HashMap<>();
            for (Map.Entry<ResourceLocation, ResourceLocation> t : e.getValue().textures().entrySet()) {
                TextureAtlasSprite sheet = sprites.apply(new Material(InventoryMenu.BLOCK_ATLAS, t.getValue()));
                if (sheet == null || MissingTextureAtlasSprite.getLocation().equals(sheet.contents().name())) {
                    LOGGER.warn("Connected textures: missing sheet {} for {}", t.getValue(), t.getKey());
                    continue;
                }
                sheets.put(t.getKey(), sheet);
            }
            if (sheets.isEmpty()) continue;
            for (BlockState state : e.getKey().getStateDefinition().getPossibleStates()) {
                ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = models.get(key);
                if (model == null || model instanceof CtBakedModel) continue;
                models.put(key, new CtBakedModel(model, sheets, e.getValue().connect()));
                wrapped++;
            }
        }
        LOGGER.info("Connected textures: {} blocks, {} block-state models", entries.size(), wrapped);
    }

    private static Map<Block, Entry> load() {
        Map<Block, Entry> out = new HashMap<>();
        var manager = Minecraft.getInstance().getResourceManager();
        for (String namespace : manager.getNamespaces()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, CONFIG);
            for (Resource resource : manager.getResourceStack(id)) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> b : root.entrySet()) {
                        Block block = block(b.getKey());
                        if (block == null) continue;
                        JsonObject cfg = b.getValue().getAsJsonObject();
                        Set<Block> connect = new HashSet<>();
                        if (cfg.has("connect")) {
                            for (JsonElement c : cfg.getAsJsonArray("connect")) {
                                Block other = block(c.getAsString());
                                if (other != null) connect.add(other);
                            }
                        } else {
                            connect.add(block);
                        }
                        Map<ResourceLocation, ResourceLocation> textures = new HashMap<>();
                        for (Map.Entry<String, JsonElement> t : cfg.getAsJsonObject("textures").entrySet()) {
                            textures.put(ResourceLocation.parse(t.getKey()), ResourceLocation.parse(t.getValue().getAsString()));
                        }
                        out.put(block, new Entry(Set.copyOf(connect), Map.copyOf(textures)));
                    }
                } catch (Exception ex) {
                    LOGGER.error("Connected textures: can't read {} from {}", id, resource.sourcePackId(), ex);
                }
            }
        }
        return out;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).orElse(null);
    }
}
