// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.hbm.backport.storage.TagValueInput;
import com.hbm.backport.storage.TagValueOutput;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 26.x block entity storage API, bridged onto 1.21.1's.
 *
 * Every block entity in the tree that extended vanilla BlockEntity directly now
 * extends this instead. It turns 1.21.1's (CompoundTag, Provider) hooks into the
 * ValueInput / ValueOutput shaped ones ntm-next overrides, so the ~240 block
 * entities keep their 26.x load/save code unchanged.
 *
 * The bridges are final: 1.21.1 calls them, the code in the tree overrides the
 * ValueInput/ValueOutput versions. A subclass's `super.loadAdditional(input)`
 * lands on the non-final overload here, which calls straight through to vanilla
 * with the underlying tag -- `super.` is not a virtual call, so there is no way
 * back into the bridge and no recursion.
 */
public abstract class BlockEntityCompat extends BlockEntity {

    protected BlockEntityCompat(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /// load ///

    @Override
    protected final void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(TagValueInput.of(tag, registries));
    }

    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(TagValueInput.tagOf(input), input.lookup());
    }

    /**
     * 26.x lets a block entity hook the whole component-aware load; 1.21.1 makes
     * that method final. This overload is the hook, and the two client-side
     * entry points that 1.21.1 routes through loadWithComponents are redirected
     * into it below.
     */
    public void loadWithComponents(ValueInput input) {
        super.loadWithComponents(TagValueInput.tagOf(input), input.lookup());
    }

    public void loadCustomOnly(ValueInput input) {
        super.loadCustomOnly(TagValueInput.tagOf(input), input.lookup());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadWithComponents(TagValueInput.of(tag, registries));
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (!tag.isEmpty()) loadWithComponents(TagValueInput.of(tag, registries));
    }

    /// save ///

    @Override
    protected final void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        saveAdditional(TagValueOutput.wrap(tag, registries));
    }

    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(TagValueOutput.tagOf(output), lookupOf(output));
    }

    @Override
    public final void removeComponentsFromTag(CompoundTag tag) {
        removeComponentsFromTag(TagValueOutput.wrap(tag, registriesOrEmpty()));
    }

    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(TagValueOutput.tagOf(output));
    }

    /// removal ///

    /**
     * 26.x's hook for a block entity's side effects just before its block is
     * removed (dropping contents, mostly). 1.21.1 has none; BlockCompat calls
     * this from onRemove when the block is actually changing. Empty by default,
     * as in 26.x: block entities that drop things override it.
     */
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    /// components ///

    @Override
    protected final void applyImplicitComponents(BlockEntity.DataComponentInput components) {
        applyImplicitComponents(new DataComponentGetter() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(net.minecraft.core.component.DataComponentType<? extends T> type) {
                return (T) components.get(type);
            }
        });
    }

    protected void applyImplicitComponents(DataComponentGetter components) {}

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
    }

    /// helpers ///

    private HolderLookup.Provider registriesOrEmpty() {
        return level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
    }

    private static HolderLookup.Provider lookupOf(ValueOutput output) {
        return TagValueOutput.lookupOf(output);
    }

    /** 26.x BlockEntity.loadWithComponents(ValueInput) on any block entity. */
    public static void loadWithComponents(net.minecraft.world.level.block.entity.BlockEntity be, ValueInput input) {
        if (be instanceof BlockEntityCompat compat) compat.loadWithComponents(input);
        else be.loadWithComponents(TagValueInput.tagOf(input), input.lookup());
    }

    /** 26.x BlockEntity.loadCustomOnly(ValueInput) on any block entity. */
    public static void loadCustomOnly(net.minecraft.world.level.block.entity.BlockEntity be, ValueInput input) {
        if (be instanceof BlockEntityCompat compat) compat.loadCustomOnly(input);
        else be.loadCustomOnly(TagValueInput.tagOf(input), input.lookup());
    }

    /** The tag form of the two above (for a ValueInput already unwrapped to its tag). */
    public static void loadWithComponents(net.minecraft.world.level.block.entity.BlockEntity be, CompoundTag tag) {
        loadWithComponents(be, TagValueInput.of(tag, lookup(be)));
    }

    public static void loadCustomOnly(net.minecraft.world.level.block.entity.BlockEntity be, CompoundTag tag) {
        loadCustomOnly(be, TagValueInput.of(tag, lookup(be)));
    }

    private static HolderLookup.Provider lookup(net.minecraft.world.level.block.entity.BlockEntity be) {
        return be.getLevel() != null ? be.getLevel().registryAccess() : com.hbm.backport.recipe.Recipes.registries();
    }
}
