// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.neoforge.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

/**
 * backport-fix: BF-132 the contents request of Sophisticated Backpacks / Storage, for every version
 * of them. The hazard tooltip asks the server for a backpack's or storage item's contents with the
 * mod's own payload. Up to Sophisticated Backpacks 3.26.17 (and the matching Storage / Core) that
 * payload took only the contents UUID; from 3.26.18 on it takes (UUID, ItemStackReference) and the
 * server answers only for a stack it finds at that reference (player inventory, carried stack or a
 * slot of the open menu). Built against the old constructor, hovering a filled backpack crashed the
 * game with NoSuchMethodError. The constructor is looked up once per payload class: the old one if
 * present, otherwise the new one with ItemStackReference.find(player, stack); a stack that can't be
 * referenced (one nested inside another backpack) isn't asked for, as the server would refuse it.
 */
final class SophisticatedContentsRequest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String REFERENCE = "net.p3pp3rf1y.sophisticatedcore.network.ItemStackReference";
    private static final Map<Class<?>, Factory> FACTORIES = new ConcurrentHashMap<>();

    @FunctionalInterface
    private interface Factory {
        /** The payload asking for {@code id}'s contents, or null when it can't be asked for. */
        CustomPacketPayload create(UUID id, ItemStack stack) throws ReflectiveOperationException;
    }

    private SophisticatedContentsRequest() {}

    static void send(Class<? extends CustomPacketPayload> payload, UUID id, ItemStack stack) {
        Factory factory = FACTORIES.computeIfAbsent(payload, SophisticatedContentsRequest::factory);
        CustomPacketPayload request;
        try {
            request = factory.create(id, stack);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Sophisticated contents request {} failed, hazard tooltips skip it", payload.getSimpleName(), e);
            FACTORIES.put(payload, (i, s) -> null);
            return;
        }
        if (request != null) PacketDistributor.sendToServer(request);
    }

    private static Factory factory(Class<?> payload) {
        {
            // before ItemStackReference: (UUID)
            Constructor<?> plain = constructor(payload, UUID.class);
            if (plain != null) return (id, stack) -> (CustomPacketPayload) plain.newInstance(id);
        }
        try {
            Class<?> reference = Class.forName(REFERENCE, false, payload.getClassLoader());
            Constructor<?> referenced = constructor(payload, UUID.class, reference);
            Method find = reference.getMethod("find", Player.class, ItemStack.class);
            if (referenced != null)
                return (id, stack) -> {
                    Player player = Minecraft.getInstance().player;
                    if (player == null) return null;
                    Optional<?> at = (Optional<?>) find.invoke(null, player, stack);
                    return at.isPresent() ? (CustomPacketPayload) referenced.newInstance(id, at.get()) : null;
                };
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("no {} for {}", REFERENCE, payload.getName(), e);
        }
        LOGGER.warn("Unknown {} constructor, NTM hazard tooltips won't ask for its contents", payload.getName());
        return (id, stack) -> null;
    }

    private static Constructor<?> constructor(Class<?> type, Class<?>... parameters) {
        for (Constructor<?> candidate : type.getConstructors())
            if (java.util.Arrays.equals(candidate.getParameterTypes(), parameters)) return candidate;
        return null;
    }
}
