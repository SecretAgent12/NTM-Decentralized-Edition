// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * 26.x {@code net.minecraft.world.item.component.Weapon}, as the data component
 * {@code hbm:backport_weapon} ({@link ToolRegistry#WEAPON}).
 *
 * <p>1.21.1 behaviour: a stack carrying it counts as having hit (ItemStack.hurtEnemy returns
 * true, ITEM_USED stat) and loses {@code itemDamagePerAttack} durability in
 * ItemStack.postHurtEnemy -- see MixinItemStackWeapon. {@code disableBlockingForSeconds} is kept
 * for data fidelity but NOT applied: 1.21.1 disables shields through NeoForge's
 * IItemExtension.canDisableShield (a fixed 5 s), and every ntm-next call site passes 0.
 */
public record Weapon(int itemDamagePerAttack, float disableBlockingForSeconds) {

    public static final float AXE_DISABLES_BLOCKING_FOR_SECONDS = 5.0F;

    public static final Codec<Weapon> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ExtraCodecs.NON_NEGATIVE_INT
                                                    .optionalFieldOf("item_damage_per_attack", 1)
                                                    .forGetter(Weapon::itemDamagePerAttack),
                                            Codec.floatRange(0.0F, Float.MAX_VALUE)
                                                    .optionalFieldOf(
                                                            "disable_blocking_for_seconds", 0.0F)
                                                    .forGetter(Weapon::disableBlockingForSeconds))
                                    .apply(i, Weapon::new));

    public static final StreamCodec<ByteBuf, Weapon> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    Weapon::itemDamagePerAttack,
                    ByteBufCodecs.FLOAT,
                    Weapon::disableBlockingForSeconds,
                    Weapon::new);

    public Weapon(int itemDamagePerAttack) {
        this(itemDamagePerAttack, 0.0F);
    }
}
