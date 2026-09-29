// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 26.x {@code net.minecraft.client.renderer.rendertype.RenderTypes}: the vanilla RenderType factories that
 * moved out of RenderType. backport: each returns the equivalent 1.21.1 vanilla RenderType. 26.x renamed
 * the entity cutout types: {@code entityCutout} is the unculled one (1.21.1 {@code entityCutoutNoCull}) and
 * {@code entityCutoutCull} the culled one (1.21.1 {@code entityCutout}).
 */
public final class RenderTypes {
    private RenderTypes() {}

    // ---- entities ----

    public static RenderType entitySolid(ResourceLocation texture) {
        return RenderType.entitySolid(texture);
    }

    /** backport: unverified: 26.x has a forward-offset solid variant; 1.21.1 has none. */
    public static RenderType entitySolidZOffsetForward(ResourceLocation texture) {
        return RenderType.entitySolid(texture);
    }

    public static RenderType entityCutout(ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    public static RenderType entityCutout(ResourceLocation texture, boolean affectsOutline) {
        return RenderType.entityCutoutNoCull(texture, affectsOutline);
    }

    public static RenderType entityCutoutCull(ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation texture, boolean affectsOutline) {
        return RenderType.entityCutoutNoCull(texture, affectsOutline);
    }

    public static RenderType entityCutoutZOffset(ResourceLocation texture) {
        return RenderType.entityCutoutNoCullZOffset(texture);
    }

    public static RenderType entityCutoutZOffset(ResourceLocation texture, boolean affectsOutline) {
        return RenderType.entityCutoutNoCullZOffset(texture, affectsOutline);
    }

    public static RenderType entityCutoutNoCullZOffset(ResourceLocation texture) {
        return RenderType.entityCutoutNoCullZOffset(texture);
    }

    public static RenderType entityTranslucent(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    public static RenderType entityTranslucent(ResourceLocation texture, boolean affectsOutline) {
        return RenderType.entityTranslucent(texture, affectsOutline);
    }

    public static RenderType entityTranslucentCull(ResourceLocation texture) {
        return RenderType.entityTranslucentCull(texture);
    }

    /** 26.x item-entity-target translucent type (1.21.1 {@code itemEntityTranslucentCull}). */
    public static RenderType entityTranslucentCullItemTarget(ResourceLocation texture) {
        return RenderType.itemEntityTranslucentCull(texture);
    }

    public static RenderType itemEntityTranslucentCull(ResourceLocation texture) {
        return RenderType.itemEntityTranslucentCull(texture);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation texture, boolean affectsOutline) {
        return RenderType.entityTranslucentEmissive(texture, affectsOutline);
    }

    public static RenderType entitySmoothCutout(ResourceLocation texture) {
        return RenderType.entitySmoothCutout(texture);
    }

    public static RenderType entityDecal(ResourceLocation texture) {
        return RenderType.entityDecal(texture);
    }

    public static RenderType entityNoOutline(ResourceLocation texture) {
        return RenderType.entityNoOutline(texture);
    }

    public static RenderType entityShadow(ResourceLocation texture) {
        return RenderType.entityShadow(texture);
    }

    public static RenderType dragonExplosionAlpha(ResourceLocation texture) {
        return RenderType.dragonExplosionAlpha(texture);
    }

    public static RenderType eyes(ResourceLocation texture) {
        return RenderType.eyes(texture);
    }

    public static RenderType breezeEyes(ResourceLocation texture) {
        return RenderType.breezeEyes(texture);
    }

    public static RenderType breezeWind(ResourceLocation texture, float u, float v) {
        return RenderType.breezeWind(texture, u, v);
    }

    public static RenderType energySwirl(ResourceLocation texture, float u, float v) {
        return RenderType.energySwirl(texture, u, v);
    }

    public static RenderType beaconBeam(ResourceLocation texture, boolean translucent) {
        return RenderType.beaconBeam(texture, translucent);
    }

    public static RenderType outline(ResourceLocation texture) {
        return RenderType.outline(texture);
    }

    public static RenderType leash() {
        return RenderType.leash();
    }

    public static RenderType waterMask() {
        return RenderType.waterMask();
    }

    public static RenderType lightning() {
        return RenderType.lightning();
    }

    public static RenderType dragonRays() {
        return RenderType.dragonRays();
    }

    public static RenderType dragonRaysDepth() {
        return RenderType.dragonRaysDepth();
    }

    public static RenderType endPortal() {
        return RenderType.endPortal();
    }

    public static RenderType endGateway() {
        return RenderType.endGateway();
    }

    public static RenderType crumbling(ResourceLocation texture) {
        return RenderType.crumbling(texture);
    }

    // ---- armor ----

    public static RenderType armorCutoutNoCull(ResourceLocation texture) {
        return RenderType.armorCutoutNoCull(texture);
    }

    public static RenderType armorDecalCutoutNoCull(ResourceLocation texture) {
        return RenderType.createArmorDecalCutoutNoCull(texture);
    }

    /** backport: 1.21.1 has no translucent armor type; the cutout one is the nearest. */
    public static RenderType armorTranslucent(ResourceLocation texture) {
        return RenderType.armorCutoutNoCull(texture);
    }

    // ---- glint ----

    public static RenderType armorEntityGlint() {
        return RenderType.armorEntityGlint();
    }

    public static RenderType glintTranslucent() {
        return RenderType.glintTranslucent();
    }

    public static RenderType glint() {
        return RenderType.glint();
    }

    public static RenderType entityGlint() {
        return RenderType.entityGlint();
    }

    public static RenderType entityGlintDirect() {
        return RenderType.entityGlintDirect();
    }

    // ---- text ----

    public static RenderType text(ResourceLocation texture) {
        return RenderType.text(texture);
    }

    public static RenderType textBackground() {
        return RenderType.textBackground();
    }

    public static RenderType textIntensity(ResourceLocation texture) {
        return RenderType.textIntensity(texture);
    }

    public static RenderType textPolygonOffset(ResourceLocation texture) {
        return RenderType.textPolygonOffset(texture);
    }

    public static RenderType textIntensityPolygonOffset(ResourceLocation texture) {
        return RenderType.textIntensityPolygonOffset(texture);
    }

    public static RenderType textSeeThrough(ResourceLocation texture) {
        return RenderType.textSeeThrough(texture);
    }

    public static RenderType textBackgroundSeeThrough() {
        return RenderType.textBackgroundSeeThrough();
    }

    public static RenderType textIntensitySeeThrough(ResourceLocation texture) {
        return RenderType.textIntensitySeeThrough(texture);
    }

    // ---- lines / debug ----

    public static RenderType lines() {
        return RenderType.lines();
    }

    /** backport: 1.21.1 {@code lines()} already blends translucently. */
    public static RenderType linesTranslucent() {
        return RenderType.lines();
    }

    /** backport: 1.21.1 draws the secondary (structure-block style) outline with the plain lines type. */
    public static RenderType secondaryBlockOutline() {
        return RenderType.lines();
    }

    public static RenderType lineStrip() {
        return RenderType.lineStrip();
    }

    public static RenderType debugLineStrip(double width) {
        return RenderType.debugLineStrip(width);
    }

    public static RenderType debugFilledBox() {
        return RenderType.debugFilledBox();
    }

    public static RenderType debugQuads() {
        return RenderType.debugQuads();
    }

    public static RenderType debugStructureQuads() {
        return RenderType.debugStructureQuads();
    }

    public static RenderType debugSectionQuads() {
        return RenderType.debugSectionQuads();
    }

    // ---- blocks ----

    public static RenderType solidMovingBlock() {
        return ChunkSectionLayer.SOLID.movingBlockRenderType();
    }

    public static RenderType cutoutMovingBlock() {
        return ChunkSectionLayer.CUTOUT.movingBlockRenderType();
    }

    public static RenderType translucentMovingBlock() {
        return RenderType.translucentMovingBlock();
    }

    public static RenderType tripwireMovingBlock() {
        return RenderType.tripwire();
    }

    public static RenderType clouds() {
        return RenderType.clouds();
    }
}
