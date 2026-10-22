// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.NuclearTech;
import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.client.ClientRegistry;
import com.hbm.client.FarView;
import com.hbm.client.ItemTooltipEvents;
import com.hbm.client.Keybinds;
import com.hbm.client.NukeHud;
import com.hbm.client.RadiationResistanceTooltip;
import com.hbm.client.WikiRenderCommand;
import com.hbm.client.ctm.CtmEngine;
import com.hbm.client.ctm.HbmCtmLoader;
import com.hbm.client.gui.FluidTooltipFrame;
import com.hbm.client.gui.PwrSliceRenderState;
import com.hbm.client.gui.PwrSliceRenderer;
import com.hbm.client.model.SectionGeometry;
import com.hbm.client.qmaw.QMAWClient;
import com.hbm.command.ConfigCommand;
import com.hbm.data.DataGroups;
import com.hbm.handler.BobmazonOffers;
import com.hbm.handler.EquipmentProtections;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.SuitProtections;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.NTMTokenFluidBase;
import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import com.hbm.inventory.machine.CustomMachineDefinitions;
import com.hbm.inventory.recipes.ModRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.items.ModItems;
import com.hbm.items.food.ConsumableTreatments;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.CrateOpenHeldPayload;
import com.hbm.particle.HbmParticleGroups;
import com.hbm.particle.HbmParticles;
import com.hbm.registration.Reg;
import com.hbm.render.loader.ObjMeshCache;
import com.hbm.wiaj.CanneryClient;
import com.hbm.wiaj.JarRenderState;
import com.hbm.wiaj.JarSceneRenderer;
import com.hbm.world.ore.OreGeneration;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.hbm.backport.client.blockmodel.BlockModels;
import com.hbm.backport.client.core.CoreClientHooks;
import com.hbm.backport.client.gui.GuiBackport;
import com.hbm.backport.client.itemmodel.ItemModelBootstrap;
import com.hbm.backport.client.misc.MiscClientHooks;
import com.hbm.backport.client.particle.ParticleBridge;
import com.hbm.backport.client.rendertype.HbmShaders;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import com.hbm.backport.client.particle.RegisterParticleGroupsEvent;
import com.hbm.backport.client.gui.render.pip.RegisterPictureInPictureRenderersEvent;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;

@Mod(value = NuclearTech.MOD_ID, dist = Dist.CLIENT)
public final class NuclearTechNeoForgeClient {
    private static final Set<ResourceLocation> NUKE_SHAKEN_LAYERS =
            Set.of(
                    VanillaGuiLayers.HOTBAR,
                    VanillaGuiLayers.JUMP_METER,
                    VanillaGuiLayers.EXPERIENCE_LEVEL,
                    VanillaGuiLayers.EXPERIENCE_BAR,
                    VanillaGuiLayers.SELECTED_ITEM_NAME,
                    VanillaGuiLayers.SLEEP_OVERLAY,
                    VanillaGuiLayers.SCOREBOARD_SIDEBAR,
                    VanillaGuiLayers.OVERLAY_MESSAGE,
                    VanillaGuiLayers.CHAT,
                    VanillaGuiLayers.TAB_LIST);
    private static boolean nukeShakePushed;

    public NuclearTechNeoForgeClient(IEventBus modBus) {
        ClientRegistry.bindClientHooks();
        // backport: 1.21.1 registration hooks of the client shims (replace the NeoForge-26 render
        // pipeline, block state model, item model, sprite source and model loader registrations)
        CoreClientHooks.register(modBus);
        HbmShaders.register(modBus);
        GuiBackport.register(modBus);
        BlockModels.register(modBus);
        ItemModelBootstrap.register(modBus);
        ParticleBridge.register(modBus);
        MiscClientHooks.register(modBus);
        modBus.addListener(
                (RegisterPictureInPictureRenderersEvent event) ->
                        event.register(PwrSliceRenderState.class, PwrSliceRenderer::new));
        modBus.addListener(
                (RegisterPictureInPictureRenderersEvent event) ->
                        event.register(JarRenderState.class, JarSceneRenderer::new));
        // backport-fix: BF-105 connected textures are built in, as in 1.7.10 (no Continuity/Fusion needed);
        // the CTM resource packs are not added any more, they would draw over the built-in ones
        com.hbm.backport.client.ct.ConnectedTextures.register(modBus);
        CtmEngine ctm = CtmEngine.NONE;
        if (ctm != CtmEngine.NONE) {
            modBus.addListener(
                    (AddPackFindersEvent event) -> {
                        event.addPackFinders(
                                Library.id(ctm.directory()),
                                PackType.CLIENT_RESOURCES,
                                ctm.title(),
                                PackSource.BUILT_IN,
                                true,
                                Pack.Position.TOP);
                        for (String namespace : Reg.addonNamespaces()) {
                            // backport: FML 4 IModFile.findResource (26.x FML getContents().containsFile)
                            if (java.nio.file.Files.exists(
                                    ModList.get()
                                            .getModFileById(namespace)
                                            .getFile()
                                            .findResource(ctm.directory(), "pack.mcmeta"))) {
                                event.addPackFinders(
                                        ResourceLocation.fromNamespaceAndPath(
                                                namespace, ctm.directory()),
                                        PackType.CLIENT_RESOURCES,
                                        ctm.title(),
                                        PackSource.BUILT_IN,
                                        true,
                                        Pack.Position.TOP);
                            }
                        }
                    });
        }
        if (ctm == CtmEngine.CONTINUITY) {
            HbmCtmLoader.register();
        }
        NeoForge.EVENT_BUS.addListener(
                (RegisterClientCommandsEvent event) ->
                        ConfigCommand.registerClient(
                                event.getDispatcher(), CommandSourceStack::sendSuccess));
        NeoForge.EVENT_BUS.addListener(
                (RegisterClientCommandsEvent event) ->
                        event.getDispatcher().register(WikiRenderCommand.build()));
        NeoForge.EVENT_BUS.addListener(
                TagsUpdatedEvent.class,
                event -> {
                    // backport: 1.21.1 has one TagsUpdatedEvent with an update cause
                    if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED)
                        return;
                    if (event.shouldUpdateStaticData()) ModRecipes.bootstrapClientRecipes();
                    GenericRecipes.invalidateIndexes();
                    HazardSystem.clearCaches();
                    NTMFluidProperties.bindEquivalents();
                });
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingIn event) -> {
                    RadiationResistanceTooltip.joined(event.getPlayer().registryAccess());
                    if (!Minecraft.getInstance().hasSingleplayerServer()) {
                        HazmatRegistry.applyDataPack(event.getPlayer().registryAccess());
                        EquipmentProtections.applyDataPack(event.getPlayer().registryAccess());
                        SuitProtections.applyDataPack(event.getPlayer().registryAccess());
                        ConsumableTreatments.applyDataPack(event.getPlayer().registryAccess());
                        HazardSystem.applyDataPack(event.getPlayer().registryAccess());
                        CustomMachineDefinitions.applyDataPack(event.getPlayer().registryAccess());
                        DataGroups.applyAll(event.getPlayer().registryAccess());
                    }
                    CrateOpenHeldPayload.report();
                    FarView.joined();
                });
        modBus.addListener((FMLClientSetupEvent event) -> NuclearTech.deriveClientConfigFacades());
        // backport: Ponder guides (com.hbm.integration.ponder), only when Ponder is present
        modBus.addListener(
                (FMLClientSetupEvent event) -> {
                    if (net.neoforged.fml.ModList.get().isLoaded("ponder"))
                        com.hbm.integration.ponder.NTMPonder.register();
                });
        modBus.addListener(
                (ModConfigEvent.Reloading event) -> {
                    if (event.getConfig().getType() != ModConfig.Type.CLIENT) return;
                    NuclearTech.deriveClientConfigFacades();
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.execute(
                            () -> {
                                if (minecraft.getConnection() != null)
                                    CrateOpenHeldPayload.report();
                            });
                });
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) ->
                        RadiationResistanceTooltip.disconnected());
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> {
                    if (!Minecraft.getInstance().hasSingleplayerServer()) {
                        HazmatRegistry.clear();
                        EquipmentProtections.clear();
                        SuitProtections.clear();
                        ConsumableTreatments.clear();
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> RecipeSource.clearClient());
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> QMAWClient.disconnected());
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> CanneryClient.disconnected());
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> OreGeneration.disconnect());
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> BobmazonOffers.invalidate());
        // backport: NeoForge 26's FluidTooltipEvent (fluid stack tooltips of recipe viewers) has no
        // 1.21.1 counterpart; the mod's own fluid tooltips call FluidTraitTooltip directly and the
        // REI plugin adds the QMAW line itself (NTMReiClientPlugin).
        NeoForgeAssemblyMarkers.register();
        NeoForgeRadVis.register();
        NeoForgeMachineSilhouette.register();
        NeoForgeRebarPlacerPreview.register();
        NeoForge.EVENT_BUS.addListener(
                (AddSectionGeometryEvent event) -> {
                    var geometry =
                            SectionGeometry.capture(event.getLevel(), event.getSectionOrigin());
                    if (geometry != null)
                        event.addRenderer(
                                context ->
                                        geometry.emit(
                                                context.getRegion(),
                                                context::getOrCreateChunkBuffer));
                });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientRegistry.clientTick());
        NeoForge.EVENT_BUS.addListener(
                (ChunkEvent.Unload event) -> {
                    if (event.getLevel().isClientSide()) {
                        FluidPipeTintData.evictChunk(
                                (Level) event.getLevel(), event.getChunk().getPos().toLong());
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ItemTooltipEvent event) ->
                        ItemTooltipEvents.append(
                                event.getItemStack(),
                                event.getEntity(),
                                event.getFlags(),
                                event.getToolTip()));
        NeoForge.EVENT_BUS.addListener(
                (ItemTooltipEvent event) ->
                        ContainedHazardTooltipSync.request(event.getItemStack()));
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> ContainedHazardTooltipSync.reset());
        modBus.addListener(
                (RegisterKeyMappingsEvent event) -> {
                    for (KeyMapping key : Keybinds.ALL) event.register(key);
                });
        modBus.addListener(
                (RegisterMenuScreensEvent event) ->
                        ClientRegistry.registerMenuScreens(event::register));
        // backport: 1.21.1 reload listeners carry no id; registration order is kept
        modBus.addListener(
                (RegisterClientReloadListenersEvent event) ->
                        event.registerReloadListener(ObjMeshCache.INSTANCE));
        modBus.addListener(
                (RegisterClientReloadListenersEvent event) ->
                        event.registerReloadListener(
                                (ResourceManagerReloadListener)
                                        ClientRegistry::reloadVisualResources));
        modBus.addListener(
                (RegisterClientReloadListenersEvent event) ->
                        event.registerReloadListener(
                                (ResourceManagerReloadListener)
                                        resourceManager -> ClientRegistry.setupGunClient()));
        modBus.addListener(
                (EntityRenderersEvent.RegisterRenderers event) -> {
                    ClientRegistry.registerBlockEntityRenderers(event::registerBlockEntityRenderer);
                    ClientRegistry.registerEntityRenderers(event::registerEntityRenderer);
                });
        // backport: item tint sources, select properties, special renderers and item models are
        // registered by ItemModelBootstrap.register above; block tint sources become 1.21.1
        // BlockColors in MiscClientHooks.register (RegisterColorHandlersEvent.Block)
        modBus.addListener(
                (RegisterClientExtensionsEvent event) -> {
                    event.registerItem(
                            GunClientExtensions.INSTANCE,
                            BuiltInRegistries.ITEM.stream()
                                    .filter(ItemGunBaseNT.class::isInstance)
                                    .toArray(Item[]::new));
                    event.registerItem(
                            GunClientExtensions.INSTANCE, ModItems.LASER_DETONATOR.get());
                });
        // backport: 26.x FluidModel.Unbaked(still, flowing, no overlay, no tint) per fluid ->
        // 1.21.1 IClientFluidTypeExtensions per FluidType (both textures are block-atlas sprites)
        modBus.addListener(
                (RegisterClientExtensionsEvent event) -> {
                    Map<FluidType, IClientFluidTypeExtensions> types = new IdentityHashMap<>();
                    BuiltInRegistries.FLUID.stream()
                            .filter(NTMTokenFluidBase.class::isInstance)
                            .forEach(
                                    fluid -> {
                                        // backport: this event fires in Minecraft's constructor, before
                                        // common setup has run NTMFluids.bake() (26.x registered fluid
                                        // models later, at model loading) -- resolve the sprite lazily
                                        java.util.function.Supplier<ResourceLocation> sprite =
                                                com.google.common.base.Suppliers.memoize(
                                                        () ->
                                                                NTMFluids.sprite(
                                                                        fluid, "block/fluid/", ""));
                                        types.putIfAbsent(fluid.getFluidType(), fluidTextures(sprite));
                                    });
                    BuiltInRegistries.FLUID.stream()
                            .filter(ClassicFluid.Source.class::isInstance)
                            .map(ClassicFluid.class::cast)
                            .forEach(
                                    fluid ->
                                            types.putIfAbsent(
                                                    fluid.getFluidType(),
                                                    fluidTextures(
                                                            fluid.spec().sprite().withSuffix("_still"),
                                                            fluid.spec()
                                                                    .sprite()
                                                                    .withSuffix("_flowing"))));
                    types.forEach(
                            (type, extensions) -> {
                                // backport: only our own fluid types -- some NTM fluids share a
                                // vanilla type (e.g. minecraft:water), whose extensions NeoForge
                                // registers itself after us and rejects as a duplicate
                                ResourceLocation key =
                                        net.neoforged.neoforge.registries.NeoForgeRegistries
                                                .FLUID_TYPES
                                                .getKey(type);
                                // backport-fix: BF-094 NTM addons' fluid types too
                                if (key == null
                                        || !com.hbm.registration.Reg.isNtmNamespace(
                                                key.getNamespace())) return;
                                if (!event.isFluidTypeRegistered(type))
                                    event.registerFluidType(extensions, type);
                            });
                });
        modBus.addListener(
                (RegisterClientTooltipComponentFactoriesEvent event) ->
                        event.register(FluidTooltipFrame.class, FluidTooltipFrame.Renderer::new));
        modBus.addListener(
                (RegisterParticleProvidersEvent event) -> {
                    event.registerSpriteSet(
                            HbmParticles.RAD_FOG.get(), ClientRegistry::radFogProvider);
                    event.registerSpriteSet(
                            HbmParticles.RBMK_MUSH.get(), ClientRegistry::rbmkMushProvider);
                    event.registerSpriteSet(
                            HbmParticles.DIGAMMA_SMOKE.get(), ClientRegistry::digammaSmokeProvider);
                    event.registerSpriteSet(
                            HbmParticles.LAUNCH_SMOKE.get(), ClientRegistry::launchSmokeProvider);
                    event.registerSpriteSet(
                            HbmParticles.VOLCANO_SMOKE.get(), ClientRegistry::volcanoSmokeProvider);
                    event.registerSpriteSet(
                            HbmParticles.SCHRAB_FOG.get(), ClientRegistry::schrabFogProvider);
                    event.registerSpriteSet(
                            HbmParticles.GAS_FLAME.get(), ClientRegistry::gasFlameProvider);
                    event.registerSpriteSet(
                            HbmParticles.HADRON.get(), ClientRegistry::hadronProvider);
                    event.registerSpriteSet(
                            HbmParticles.BLACK_POWDER_SPARK.get(),
                            ClientRegistry::blackPowderSparkProvider);
                    event.registerSpriteSet(
                            HbmParticles.MIST_TOWER.get(), ClientRegistry::mistTowerProvider);
                    event.registerSpriteSet(
                            HbmParticles.SPLASH.get(), ClientRegistry::splashProvider);
                    event.registerSpriteSet(
                            HbmParticles.FLAME.get(), ClientRegistry::flameProvider);
                    event.registerSpriteSet(
                            HbmParticles.ASH_REVEAL.get(), ClientRegistry::ashRevealProvider);
                });
        modBus.addListener(
                (RegisterParticleGroupsEvent event) -> HbmParticleGroups.forEach(event::register));
        modBus.addListener(
                (FMLClientSetupEvent event) ->
                        event.enqueueWork(ClientRegistry::registerFlywheelVisualizers));
        modBus.addListener(
                (RegisterGuiLayersEvent event) -> {
                    event.registerBelow(
                            VanillaGuiLayers.ARMOR_LEVEL,
                            ClientRegistry.SHIELD_BAR_ID,
                            (graphics, deltaTracker) -> {
                                var hud = Minecraft.getInstance().gui;
                                if (ClientRegistry.renderShieldBar(
                                        GuiGraphicsExtractor.of(graphics), graphics.guiHeight() - hud.leftHeight))
                                    hud.leftHeight += 10;
                            });
                    event.registerAboveAll(
                            ClientRegistry.LOOK_OVERLAY_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderLookOverlay(GuiGraphicsExtractor.of(graphics)));
                    event.registerAboveAll(
                            ClientRegistry.RADIATION_HUD_ID,
                            (graphics, deltaTracker) ->
                                    ClientRegistry.renderRadiationHud(GuiGraphicsExtractor.of(graphics)));
                    event.registerAboveAll(
                            ClientRegistry.RADVIS_HUD_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderRadVisHud(GuiGraphicsExtractor.of(graphics)));
                    event.registerBelow(
                            VanillaGuiLayers.CROSSHAIR,
                            ClientRegistry.GUN_SCOPE_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderGunScope(GuiGraphicsExtractor.of(graphics)));
                    event.registerBelow(
                            VanillaGuiLayers.CROSSHAIR,
                            ClientRegistry.NUKE_FLASH_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderNukeFlash(GuiGraphicsExtractor.of(graphics)));
                    event.registerBelow(
                            VanillaGuiLayers.CROSSHAIR,
                            ClientRegistry.INFO_HUD_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderInfoHud(GuiGraphicsExtractor.of(graphics)));
                    event.registerBelowAll(
                            ClientRegistry.ARMOR_OVERLAY_ID,
                            (graphics, deltaTracker) ->
                                    ClientRegistry.renderArmorOverlay(GuiGraphicsExtractor.of(graphics)));
                    event.registerBelow(
                            VanillaGuiLayers.CROSSHAIR,
                            ClientRegistry.TOOL_ABILITY_HUD_ID,
                            (graphics, deltaTracker) ->
                                    ClientRegistry.renderToolAbilityHud(GuiGraphicsExtractor.of(graphics)));
                    event.registerBelow(
                            VanillaGuiLayers.HOTBAR,
                            ClientRegistry.GUN_HUD_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderGunHud(GuiGraphicsExtractor.of(graphics)));
                    event.registerAboveAll(
                            ClientRegistry.HEV_HUD_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderHevHud(GuiGraphicsExtractor.of(graphics)));
                    event.registerAboveAll(
                            ClientRegistry.DASH_BAR_ID,
                            (graphics, deltaTracker) -> ClientRegistry.renderDashBar(GuiGraphicsExtractor.of(graphics)));
                });
        NeoForge.EVENT_BUS.addListener(
                (RenderGuiLayerEvent.Pre event) -> {
                    if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)
                            && ClientRegistry.replacesCrosshair()) {
                        event.setCanceled(true);
                        ClientRegistry.renderGunCrosshair(GuiGraphicsExtractor.of(event.getGuiGraphics()));
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                (RenderGuiLayerEvent.Pre event) -> {
                    if (NUKE_SHAKEN_LAYERS.contains(event.getName())) {
                        nukeShakePushed = NukeHud.pushShake(GuiGraphicsExtractor.of(event.getGuiGraphics()));
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                EventPriority.HIGHEST,
                (RenderGuiLayerEvent.Post event) -> {
                    if (nukeShakePushed && NUKE_SHAKEN_LAYERS.contains(event.getName())) {
                        NukeHud.popShake(GuiGraphicsExtractor.of(event.getGuiGraphics()));
                        nukeShakePushed = false;
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (RenderGuiLayerEvent.Pre event) -> {
                    if ((event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)
                                    || event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL))
                            && ClientRegistry.replacesVanillaVitals()) {
                        event.setCanceled(true);
                    }
                });
        // backport: the NeoForge-26 model loader (hbm:obj), block state definitions (hbm:baked, hbm:ctm)
        // and sprite sources are registered by BlockModels.register above; CTM Lib has no 1.21.1 build
    }

    private static IClientFluidTypeExtensions fluidTextures(
            java.util.function.Supplier<ResourceLocation> sprite) {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return sprite.get();
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return sprite.get();
            }
        };
    }

    private static IClientFluidTypeExtensions fluidTextures(
            ResourceLocation still, ResourceLocation flowing) {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }
        };
    }
}
