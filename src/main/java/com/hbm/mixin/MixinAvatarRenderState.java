// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.client.core.ClientAsset;
import com.hbm.client.render.PlayerCapes;
import com.hbm.main.Polaroid;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * backport: 1.21.1 has no AvatarRenderState. Its 26.x appearance flags are read from the player
 * where they are used (MixinAvatarRenderer, MixinLivingEntityRenderer, ManlyPlayerLayer); the HBM
 * cape that 26.x wrote into {@code state.skin} is applied to the skin 1.21.1's CapeLayer and
 * ElytraLayer read, {@code AbstractClientPlayer#getSkin()}.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class MixinAvatarRenderState {

    @ModifyReturnValue(method = "getSkin()Lnet/minecraft/client/resources/PlayerSkin;", at = @At("RETURN"))
    private PlayerSkin hbm$cape(PlayerSkin skin) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        ClientAsset.ResourceTexture cape =
                PlayerCapes.forPlayer(
                        player.getUUID(),
                        player.getDisplayName().getString(),
                        Polaroid.isBalefireDay());
        if (cape == null) return skin;
        return new PlayerSkin(
                skin.texture(),
                skin.textureUrl(),
                cape.texturePath(),
                skin.elytraTexture(),
                skin.model(),
                skin.secure());
    }
}
